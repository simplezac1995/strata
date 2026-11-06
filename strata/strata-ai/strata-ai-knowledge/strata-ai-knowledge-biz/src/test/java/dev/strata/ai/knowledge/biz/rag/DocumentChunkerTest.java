// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DocumentChunker 切片")
class DocumentChunkerTest {

    private final DocumentChunker chunker = new DocumentChunker(500, 50);

    @Test
    @DisplayName("空文本返回空列表")
    void blank_text_yields_no_chunks() {
        assertThat(chunker.chunk(null)).isEmpty();
        assertThat(chunker.chunk("   ")).isEmpty();
    }

    @Test
    @DisplayName("短文本单段成一个 chunk")
    void short_text_single_chunk() {
        List<String> chunks = chunker.chunk("这是一段很短的文档。");
        assertThat(chunks).containsExactly("这是一段很短的文档。");
    }

    @Test
    @DisplayName("超长段落按窗口切分且保留重叠")
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
    @DisplayName("多段落合并进窗口，溢出时携带重叠")
    void paragraphs_pack_with_overlap() {
        String p1 = "短段落一。";
        String p2 = "短段落二。";
        String p3 = "短段落三。";
        List<String> chunks = chunker.chunk(p1 + "\n" + p2 + "\n" + p3);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains(p1, p2, p3);
    }
}
