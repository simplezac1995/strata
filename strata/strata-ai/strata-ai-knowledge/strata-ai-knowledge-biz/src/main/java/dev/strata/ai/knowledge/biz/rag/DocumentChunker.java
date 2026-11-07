// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.rag;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Pure-Java text slicer for RAG ingestion — no external dependency.
 *
 * <p>Strategy (task spec): split on paragraphs first, then pack paragraphs into a sliding window of
 * {@code window} characters with {@code overlap} characters carried over from the previous chunk so sentence
 * boundaries are not lost across the seam. Paragraphs longer than the window are sub-windowed with the same
 * window/overlap.
 */
@Component
public final class DocumentChunker {

    /** Default window size in characters. */
    public static final int DEFAULT_WINDOW = 500;
    /** Default overlap carried between adjacent chunks. */
    public static final int DEFAULT_OVERLAP = 50;

    private final int window;
    private final int overlap;

    public DocumentChunker() {
        this(DEFAULT_WINDOW, DEFAULT_OVERLAP);
    }

    public DocumentChunker(int window, int overlap) {
        this.window = Math.max(50, window);
        this.overlap = Math.max(0, Math.min(overlap, this.window - 1));
    }

    /**
     * Split {@code text} into chunks. Blank text yields an empty list. Leading/trailing whitespace is trimmed
     * and CRLF is normalized.
     */
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String normalized = text.replace("\r\n", "\n").trim();
        String[] paragraphs = normalized.split("\\n");
        StringBuilder buffer = new StringBuilder();
        for (String raw : paragraphs) {
            String paragraph = raw == null ? "" : raw.trim();
            if (paragraph.isEmpty()) {
                continue;
            }
            if (buffer.length() == 0) {
                buffer.append(paragraph);
                continue;
            }
            int joined = buffer.length() + 1 + paragraph.length();
            if (joined <= window) {
                buffer.append('\n').append(paragraph);
            } else {
                flush(buffer.toString(), chunks);
                String carry = tailOverlap(buffer);
                buffer.setLength(0);
                buffer.append(carry).append(paragraph);
            }
        }
        if (buffer.length() > 0) {
            flush(buffer.toString(), chunks);
        }
        return chunks;
    }

    /** Add {@code segment}, sub-windowing it when it exceeds the configured window. */
    private void flush(String segment, List<String> out) {
        if (segment.length() <= window) {
            out.add(segment);
            return;
        }
        int start = 0;
        while (start < segment.length()) {
            int end = Math.min(start + window, segment.length());
            out.add(segment.substring(start, end));
            if (end == segment.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
    }

    /** Last {@link #overlap} characters of the previous chunk, prepended to the new chunk. */
    private String tailOverlap(StringBuilder previous) {
        if (previous.length() <= overlap) {
            return previous.toString() + "\n";
        }
        return previous.substring(previous.length() - overlap) + "\n";
    }
}
