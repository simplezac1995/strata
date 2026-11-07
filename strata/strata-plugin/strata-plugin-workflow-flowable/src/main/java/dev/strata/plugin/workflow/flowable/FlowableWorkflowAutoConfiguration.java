// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.workflow.flowable;

import dev.strata.plugin.api.workflow.ProcessDefinitionCatalog;
import dev.strata.plugin.api.workflow.WorkflowEngine;
import javax.sql.DataSource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration
@AutoConfigureBefore(name = "dev.strata.enterprise.config.EnterpriseAutoConfiguration")
@ConditionalOnProperty(prefix = "strata.plugin.workflow.flowable", name = "enabled", havingValue = "true")
public class FlowableWorkflowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SpringProcessEngineConfiguration.class)
    SpringProcessEngineConfiguration processEngineConfiguration(
            DataSource dataSource, PlatformTransactionManager transactionManager) throws java.io.IOException {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        configuration.setDatabaseSchemaUpdate(SpringProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        configuration.setAsyncExecutorActivate(false);
        configuration.setDeploymentResources(
                new PathMatchingResourcePatternResolver().getResources("classpath*:/flowable/*.bpmn20.xml"));
        return configuration;
    }

    @Bean
    @ConditionalOnMissingBean(ProcessEngine.class)
    ProcessEngineFactoryBean processEngineFactory(SpringProcessEngineConfiguration configuration) {
        ProcessEngineFactoryBean factory = new ProcessEngineFactoryBean();
        factory.setProcessEngineConfiguration(configuration);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(ProcessEngine.class)
    ProcessEngine processEngine(ProcessEngineFactoryBean processEngineFactory) throws Exception {
        return processEngineFactory.getObject();
    }

    @Bean
    RuntimeService flowableRuntimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    HistoryService flowableHistoryService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    @Bean
    RepositoryService flowableRepositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    @ConditionalOnMissingBean(WorkflowEngine.class)
    WorkflowEngine flowableWorkflowEngine(RuntimeService runtimeService, HistoryService historyService) {
        return new FlowableWorkflowEngine(runtimeService, historyService);
    }

    @Bean
    @ConditionalOnMissingBean(ProcessDefinitionCatalog.class)
    ProcessDefinitionCatalog flowableProcessDefinitionCatalog(RepositoryService repositoryService) {
        return new FlowableProcessDefinitionCatalog(repositoryService);
    }
}
