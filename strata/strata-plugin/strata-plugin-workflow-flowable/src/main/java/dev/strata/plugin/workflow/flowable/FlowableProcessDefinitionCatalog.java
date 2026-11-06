// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.workflow.flowable;

import dev.strata.plugin.api.workflow.ProcessDefinitionCatalog;
import dev.strata.plugin.api.workflow.ProcessDefinitionDto;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;

public class FlowableProcessDefinitionCatalog implements ProcessDefinitionCatalog {

    private final RepositoryService repositoryService;

    public FlowableProcessDefinitionCatalog(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<ProcessDefinitionDto> list() {
        return repositoryService
                .createProcessDefinitionQuery()
                .latestVersion()
                .orderByProcessDefinitionKey()
                .asc()
                .list()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public Optional<String> getBpmnXml(String key) {
        ProcessDefinition definition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .latestVersion()
                .singleResult();
        if (definition == null) {
            return Optional.empty();
        }
        try (InputStream input =
                repositoryService.getResourceAsStream(definition.getDeploymentId(), definition.getResourceName())) {
            if (input == null) {
                return Optional.empty();
            }
            return Optional.of(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private ProcessDefinitionDto toDto(ProcessDefinition definition) {
        return new ProcessDefinitionDto(definition.getKey(), definition.getName(), definition.getVersion());
    }

    @Override
    public Optional<ProcessDefinitionDto> deploy(String name, String bpmnXml) {
        if (name == null || name.isBlank() || bpmnXml == null || bpmnXml.isBlank()) {
            return Optional.empty();
        }
        String resourceName = name.endsWith(".bpmn20.xml") ? name : name + ".bpmn20.xml";
        Deployment deployment = repositoryService
                .createDeployment()
                .name(name)
                .addString(resourceName, bpmnXml)
                .deploy();
        ProcessDefinition definition = repositoryService
                .createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        if (definition == null) {
            return Optional.empty();
        }
        return Optional.of(toDto(definition));
    }
}
