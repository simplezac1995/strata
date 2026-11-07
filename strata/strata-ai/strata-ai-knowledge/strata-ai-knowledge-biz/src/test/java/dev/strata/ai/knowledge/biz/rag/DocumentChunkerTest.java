// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentChunkerTest {

    private final DocumentChunker chunker = new DocumentChunker(500, 50);

    @Test
    void blank_text_yields_no_chunks() {
        assertThat(chunker.chunk(null)).isEmpty();
        assertThat(chunker.chunk("   ")).isEmpty();
    }

    @Test
    void short_text_single_chunk() {
        List<String> chunks = chunker.chunk("这是一段很短的文档。");
        assertThat(chunks).containsExactly("这是一段很短的文档。");
    }

    @Test
    void long_paragraph_is_windowed_with_overlap() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1200; i++) {
            longText.append('a');
        }
        List<String> chunks = chunker.chunk(longText.toString());
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(500);
        assertThat(chunks.get(1)).startsWith("a".repeat(50));
    }

    @Test
    void paragraphs_pack_with_overlap() {
        String p1 = "短段落一。";
        String p2 = "短段落二。";
        String p3 = "短段落三。";
        List<String> chunks = chunker.chunk(p1 + "\n" + p2 + "\n" + p3);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains(p1, p2, p3);
    }
}
