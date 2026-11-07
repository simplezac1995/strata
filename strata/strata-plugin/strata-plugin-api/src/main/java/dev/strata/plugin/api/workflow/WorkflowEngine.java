// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.workflow;

import java.util.Map;
import java.util.Optional;

/**
 * BPM / workflow engine SPI (Enterprise). Community edition uses metadata + stub runner only.
 */
public interface WorkflowEngine {

    /** Whether a real engine (e.g. Flowable) is active. */
    boolean isAvailable();

    /** Start a process instance from a deployed definition key. */
    String startProcess(String definitionKey, Map<String, Object> variables);

    /**
     * Poll engine status for a process instance id returned by {@link #startProcess}.
     * Returns RUNNING, COMPLETED, or FAILED; empty when unknown.
     */
    Optional<String> pollStatus(String processInstanceId);
}
