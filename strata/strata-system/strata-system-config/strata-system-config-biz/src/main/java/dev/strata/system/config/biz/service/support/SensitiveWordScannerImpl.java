// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.service.support;

import dev.strata.system.api.support.SensitiveWordScanner;
import dev.strata.system.config.api.entity.SensitiveWord;
import dev.strata.system.config.biz.repository.SensitiveWordRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SensitiveWordScannerImpl implements SensitiveWordScanner {

    private final SensitiveWordRepository sensitiveWordRepository;

    public SensitiveWordScannerImpl(SensitiveWordRepository sensitiveWordRepository) {
        this.sensitiveWordRepository = sensitiveWordRepository;
    }

    @Override
    public String scanHits(String content) {
        List<String> hits = new ArrayList<>();
        String lower = content.toLowerCase(Locale.ROOT);
        for (SensitiveWord word : sensitiveWordRepository.findAll()) {
            if (!word.isEnabled()) {
                continue;
            }
            if (word.getWord() != null
                    && !word.getWord().isBlank()
                    && lower.contains(word.getWord().toLowerCase(Locale.ROOT))) {
                hits.add("- " + word.getWord());
            }
        }
        return hits.isEmpty() ? "" : String.join("\n", hits);
    }
}
