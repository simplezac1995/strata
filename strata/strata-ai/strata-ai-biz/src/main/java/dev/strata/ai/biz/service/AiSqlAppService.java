// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import dev.strata.ai.api.dto.Chat2SqlResponse;
import dev.strata.ai.api.request.Chat2SqlRequest;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.llm.LlmChatCommand;
import dev.strata.plugin.api.llm.LlmChatMessage;
import dev.strata.plugin.api.llm.LlmChatResult;
import dev.strata.plugin.api.llm.LlmGateway;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Natural-language → read-only SQL round-trip.
 *
 * <ol>
 *   <li>Ask the LLM (system prompt pins SELECT-only PostgreSQL generation against the live schema or
 *       {@code schemaHint});
 *   <li>extract the first {@code SELECT} from the reply;
 *   <li>enforce the read-only contract (top-level SELECT / CTE, write-keyword blacklist);
 *   <li>execute on the application datasource inside a read-only transaction, capped at 100 rows;
 *   <li>shape columns + rows and infer a {@code chartSuggestion} from the column types.
 * </ol>
 *
 * <p>The SQL guard is defense in depth: even a model that emits DML is rejected before it reaches the
 * datasource.
 */
@Service
public class AiSqlAppService {

    private static final Logger log = LoggerFactory.getLogger(AiSqlAppService.class);

    /** Hard row cap returned to the frontend. */
    static final int MAX_ROWS = 100;

    private final LlmGateway llmGateway;
    private final AiProviderRepository aiProviderRepository;
    private final AiModelRepository aiModelRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate readOnlyTx;
    private final AiUsageRecorder usageRecorder;

    public AiSqlAppService(
            LlmGateway llmGateway,
            AiProviderRepository aiProviderRepository,
            AiModelRepository aiModelRepository,
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            AiUsageRecorder usageRecorder) {
        this.llmGateway = llmGateway;
        this.aiProviderRepository = aiProviderRepository;
        this.aiModelRepository = aiModelRepository;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.setMaxRows(MAX_ROWS);
        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);
        readOnly.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        this.readOnlyTx = readOnly;
        this.usageRecorder = usageRecorder;
    }

    @Transactional(readOnly = true)
    public Chat2SqlResponse chat2Sql(Chat2SqlRequest request) {
        ResolvedLlm llm = resolveLlm();
        String question = request.question() == null ? "" : request.question().trim();
        if (question.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Question must not be blank");
        }

        String liveSchema = describeLiveSchema(request.schemaHint());
        String systemPrompt = Chat2SqlSupport.buildSystemPrompt(liveSchema);
        LlmChatCommand command = new LlmChatCommand(
                llm.provider().getBaseUrl(),
                llm.provider().getApiKey(),
                llm.model().getCode(),
                List.of(new LlmChatMessage("system", systemPrompt)),
                question);
        LlmChatResult llmResult = llmGateway.chatWithUsage(command);
        String raw = llmResult.content();
        usageRecorder.record(llm.provider().getCode(), llm.model().getCode(), null, "sql", llmResult);

        String sql;
        try {
            sql = Chat2SqlSupport.extractSelect(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Model did not return a SELECT query");
        }
        if (!Chat2SqlSupport.isReadOnlySelect(sql)) {
            log.warn("Rejected non read-only SQL from Chat2SQL: {}", sql);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only SELECT queries are allowed");
        }

        QueryResult result = executeReadOnly(sql);
        String chart = Chat2SqlSupport.suggestChart(result.columns());
        return new Chat2SqlResponse(sql, result.columns(), result.rows(), chart);
    }

    /**
     * Run the guarded SELECT inside a read-only transaction. Package-private so unit tests can substitute a
     * canned result without a real datasource.
     */
    QueryResult executeReadOnly(String sql) {
        return readOnlyTx.execute(status -> jdbcTemplate.query(sql, new RowExtractor()));
    }

    private ResolvedLlm resolveLlm() {
        AiProvider provider = aiProviderRepository
                .findFirstByEnabledTrueOrderByIdAsc()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "No enabled AI provider configured"));
        AiModel model = aiModelRepository
                .findFirstByEnabledTrueAndProviderIdOrderByIdAsc(provider.getId())
                .or(() -> aiModelRepository.findFirstByEnabledTrueOrderByIdAsc())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "No enabled AI model configured"));
        return new ResolvedLlm(provider, model);
    }

    private record ResolvedLlm(AiProvider provider, AiModel model) {}

    /**
     * Pull the real column list for key tables from INFORMATION_SCHEMA so the LLM never invents columns.
     * Falls back to the caller-supplied hint on any error.
     */
    private String describeLiveSchema(String hint) {
        try {
            List<String> tables = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE()", String.class);
            StringBuilder sb = new StringBuilder("Live schema (columns per actual table):\n");
            for (String t : tables) {
                sb.append("- ").append(t).append("(");
                List<String> cols = jdbcTemplate.queryForList(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                        String.class,
                        t);
                sb.append(String.join(", ", cols));
                sb.append(")\n");
            }
            return sb.toString();
        } catch (Exception ex) {
            log.warn("Failed to introspect live schema, falling back to hint", ex);
            return hint == null ? "" : hint;
        }
    }

    /** Columns + rows of an executed read-only query. */
    /** Columns + rows of an executed read-only query. */
    record QueryResult(List<String> columns, List<java.util.Map<String, Object>> rows) {}

    /** Maps a {@link ResultSet} into {@link QueryResult}, honoring the row cap. */
    private static final class RowExtractor implements ResultSetExtractor<QueryResult> {

        @Override
        public QueryResult extractData(ResultSet rs) throws SQLException {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            List<String> columns = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }
            List<java.util.Map<String, Object>> rows = new ArrayList<>();
            while (rs.next() && rows.size() < MAX_ROWS) {
                java.util.Map<String, Object> row = new java.util.LinkedHashMap<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
            return new QueryResult(columns, rows);
        }
    }

    /** List all user tables in the current database. */
    public List<java.util.Map<String, Object>> listTables() {
        return jdbcTemplate.queryForList(
                "SELECT TABLE_NAME AS tableName, TABLE_COMMENT AS tableComment, TABLE_ROWS AS tableRows "
                        + "FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() "
                        + "AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME");
    }

    /** List columns of a given table. */
    public List<java.util.Map<String, Object>> listColumns(String table) {
        return jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME AS columnName, COLUMN_TYPE AS columnType, "
                        + "COLUMN_COMMENT AS columnComment, COLUMN_KEY AS columnKey, "
                        + "IS_NULLABLE AS isNullable, COLUMN_DEFAULT AS columnDefault "
                        + "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                        + "AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                table);
    }
}
