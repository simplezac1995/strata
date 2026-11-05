// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.workflow;

import java.util.List;
import java.util.Optional;

/** Read-only catalog of deployed BPMN process definitions (Enterprise / Flowable). */
public interface ProcessDefinitionCatalog {

    boolean isAvailable();

    List<ProcessDefinitionDto> list();

    Optional<String> getBpmnXml(String key);

    /** Deploy or update a BPMN definition (Flowable only). */
    Optional<ProcessDefinitionDto> deploy(String name, String bpmnXml);
}
