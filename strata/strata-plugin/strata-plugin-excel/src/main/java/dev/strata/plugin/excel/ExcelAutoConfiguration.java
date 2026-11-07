// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.excel;

import dev.strata.plugin.api.spreadsheet.SpreadsheetExporter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "strata.plugin.excel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExcelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SpreadsheetExporter.class)
    SpreadsheetExporter poiSpreadsheetExporter() {
        return new PoiSpreadsheetExporter();
    }
}
