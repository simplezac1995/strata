// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.service;

import dev.strata.ai.api.request.AiMcpServerCreateRequest;
import dev.strata.ai.api.request.AiMcpServerUpdateRequest;
import dev.strata.ai.config.api.dto.AiMcpServerDto;
import dev.strata.ai.config.api.entity.AiMcpServer;
import dev.strata.ai.config.biz.mapper.AiMcpServerMapper;
import dev.strata.ai.config.biz.repository.AiMcpServerRepository;
import dev.strata.common.core.audit.OperationLog;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.common.core.web.PageRequests;
import dev.strata.common.core.web.PageResult;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiMcpServerAppService {

    private static final Logger log = LoggerFactory.getLogger(AiMcpServerAppService.class);

    private final AiMcpServerRepository aiMcpServerRepository;
    private final AiMcpServerMapper aiMcpServerMapper;

    public AiMcpServerAppService(AiMcpServerRepository aiMcpServerRepository, AiMcpServerMapper aiMcpServerMapper) {
        this.aiMcpServerRepository = aiMcpServerRepository;
        this.aiMcpServerMapper = aiMcpServerMapper;
    }

    @Transactional(readOnly = true)
    public List<AiMcpServerDto> list() {
        return aiMcpServerRepository.findAllByOrderByIdAsc().stream()
                .map(aiMcpServerMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResult<AiMcpServerDto> page(int page, int size) {
        return PageResult.from(aiMcpServerRepository
                .findAll(PageRequests.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .map(aiMcpServerMapper::toDto));
    }

    @Transactional(readOnly = true)
    public AiMcpServerDto get(Long id) {
        return aiMcpServerMapper.toDto(require(id));
    }

    @Transactional
    @OperationLog(module = "ai:mcp", action = "create")
    public AiMcpServerDto create(AiMcpServerCreateRequest request) {
        AiMcpServer server = aiMcpServerMapper.toEntity(request);
        aiMcpServerRepository.save(server);
        return aiMcpServerMapper.toDto(server);
    }

    @Transactional
    @OperationLog(module = "ai:mcp", action = "update")
    public AiMcpServerDto update(Long id, AiMcpServerUpdateRequest request) {
        AiMcpServer server = require(id);
        aiMcpServerMapper.update(request, server);
        aiMcpServerRepository.save(server);
        return aiMcpServerMapper.toDto(server);
    }

    @Transactional
    @OperationLog(module = "ai:mcp", action = "delete")
    public void delete(Long id) {
        aiMcpServerRepository.delete(require(id));
    }

    /** Test connection: send MCP initialize handshake, return server info or error. */
    public Map<String, Object> testConnection(Long id) {
        AiMcpServer server = require(id);
        try {
            var client = mcpClient();
            var initBody = Map.of(
                    "jsonrpc",
                    "2.0",
                    "id",
                    1,
                    "method",
                    "initialize",
                    "params",
                    Map.of(
                            "protocolVersion", "2024-11-05",
                            "capabilities", Map.of(),
                            "clientInfo", Map.of("name", "strata", "version", "1.0")));
            var resp = client.post()
                    .uri(URI.create(normalizeUrl(server.getUrl())))
                    .headers(h -> applyAuth(h, server))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(initBody)
                    .retrieve()
                    .body(String.class);
            return Map.of("ok", true, "response", resp == null ? "" : resp.substring(0, Math.min(500, resp.length())));
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    /** List tools exposed by this MCP server (tools/list). */
    public List<Map<String, Object>> listTools(Long id) {
        AiMcpServer server = require(id);
        try {
            var client = mcpClient();
            var body = Map.of("jsonrpc", "2.0", "id", 2, "method", "tools/list", "params", Map.of());
            var resp = client.post()
                    .uri(URI.create(normalizeUrl(server.getUrl())))
                    .headers(h -> applyAuth(h, server))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            var tree = new tools.jackson.databind.ObjectMapper().readTree(resp);
            var tools = tree.path("result").path("tools");
            List<Map<String, Object>> out = new java.util.ArrayList<>();
            if (tools.isArray()) {
                for (var t : tools) {
                    out.add(Map.of(
                            "name", t.path("name").asText(""),
                            "description", t.path("description").asText("")));
                }
            }
            return out;
        } catch (Exception e) {
            log.warn("MCP listTools failed for {}: {}", server.getName(), e.getMessage());
            return List.of();
        }
    }

    private static String normalizeUrl(String url) {
        if (url == null) return "";
        String u = url.trim();
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    private static void applyAuth(org.springframework.http.HttpHeaders headers, AiMcpServer server) {
        if (server.getAuthToken() != null && !server.getAuthToken().isBlank()) {
            String type = server.getAuthType() == null
                    ? "bearer"
                    : server.getAuthType().toLowerCase();
            if ("basic".equals(type)) {
                headers.setBasicAuth(server.getAuthToken(), "");
            } else {
                headers.setBearerAuth(server.getAuthToken());
            }
        }
    }

    private static org.springframework.web.client.RestClient mcpClient() {
        var f = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        f.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        f.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return org.springframework.web.client.RestClient.builder()
                .requestFactory(f)
                .build();
    }

    private AiMcpServer require(Long id) {
        return aiMcpServerRepository.findById(id).orElseThrow(() -> new NotFoundException("MCP server not found"));
    }
}
