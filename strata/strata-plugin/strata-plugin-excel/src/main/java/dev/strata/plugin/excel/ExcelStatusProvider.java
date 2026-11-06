// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.excel;

import dev.strata.plugin.api.spreadsheet.SpreadsheetExporter;
import dev.strata.plugin.api.status.PluginStatusProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Self-declares the {@code excel} status key (ADR-0020).
 *
 * <p>Registered while the Excel module is enabled — mirroring {@link ExcelAutoConfiguration}
 * (default on, {@code matchIfMissing}) — so the key is omitted from {@code /plugins/status} only
 * when the switch is explicitly off. Availability tracks the {@link SpreadsheetExporter} bean the
 * module provides.
 */
@Component
@Order(40)
@ConditionalOnProperty(prefix = "strata.plugin.excel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExcelStatusProvider implements PluginStatusProvider {

    private final ObjectProvider<SpreadsheetExporter> spreadsheetExporter;

    public ExcelStatusProvider(ObjectProvider<SpreadsheetExporter> spreadsheetExporter) {
        this.spreadsheetExporter = spreadsheetExporter;
    }

    @Override
    public String key() {
        return "excel";
    }

    @Override
    public String label() {
        return "Excel export";
    }

    @Override
    public boolean available() {
        return spreadsheetExporter.getIfAvailable() != null;
    }

    @Override
    public String detail() {
        return "Apache POI xlsx export";
    }
}
