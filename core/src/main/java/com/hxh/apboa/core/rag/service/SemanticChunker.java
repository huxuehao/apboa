package com.hxh.apboa.core.rag.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * 描述：语义分块编排，优先按结构块切分，再按配置合并，尽量避免拆散表格、标题和行记录。
 *
 * @author huxuehao
 */
@Component
public class SemanticChunker {

    public record SemanticOptions(String thresholdStrategy,
                                  double thresholdValue,
                                  int windowSize,
                                  int minChunkSize,
                                  int maxChunkSize) {
        public static SemanticOptions defaults(int chunkSize) {
            return new SemanticOptions("FIXED", 0.6d, 1, 0, chunkSize);
        }
    }

    public record StructuredChunk(String content, String role, String parentContent, float[] embedding) {
        public StructuredChunk(String content, String role, String parentContent) {
            this(content, role, parentContent, null);
        }
    }

    public List<TextChunker.ChunkResult> chunk(String text,
                                               String fileName,
                                               String parserType,
                                               String chunkStrategy,
                                               int chunkSize,
                                               int overlap,
                                               List<String> delimiters) {
        return toChunkResults(chunkWithMetadata(text, fileName, parserType, chunkStrategy, chunkSize, overlap, delimiters), overlap);
    }

    public List<StructuredChunk> chunkWithMetadata(String text,
                                                   String fileName,
                                                   String parserType,
                                                   String chunkStrategy,
                                                   int chunkSize,
                                                   int overlap,
                                                   List<String> delimiters) {
        return chunkWithMetadata(text, fileName, parserType, chunkStrategy, chunkSize, overlap, delimiters, null);
    }

    public List<StructuredChunk> chunkWithMetadata(String text,
                                                   String fileName,
                                                   String parserType,
                                                   String chunkStrategy,
                                                   int chunkSize,
                                                   int overlap,
                                                   List<String> delimiters,
                                                   Function<List<String>, List<float[]>> embeddingProvider) {
        return chunkWithMetadata(
                text,
                fileName,
                parserType,
                chunkStrategy,
                chunkSize,
                overlap,
                delimiters,
                embeddingProvider,
                SemanticOptions.defaults(chunkSize)
        );
    }

    public List<StructuredChunk> chunkWithMetadata(String text,
                                                   String fileName,
                                                   String parserType,
                                                   String chunkStrategy,
                                                   int chunkSize,
                                                   int overlap,
                                                   List<String> delimiters,
                                                   Function<List<String>, List<float[]>> embeddingProvider,
                                                   SemanticOptions semanticOptions) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String strategy = normalize(chunkStrategy, "CHARACTER");
        if ("SEPARATOR".equals(strategy)) {
            return toStructuredChunks(splitByDelimiters(text, delimiters), "SEPARATOR");
        }
        if ("SEMANTIC".equals(strategy)) {
            return buildSemanticChunks(text, embeddingProvider, semanticOptions);
        }
        if ("MARKDOWN".equals(strategy)) {
            return toStructuredChunks(splitMarkdownBlocks(text), "MARKDOWN");
        }
        if (isStructuredFile(fileName, parserType)) {
            return toStructuredChunks(splitStructuredBlocks(text), "STRUCTURED");
        }
        return toStructuredChunks(splitParagraphBlocks(text), "CHARACTER");
    }

    private List<TextChunker.ChunkResult> toChunkResults(List<StructuredChunk> chunks, int overlap) {
        List<TextChunker.ChunkResult> results = new ArrayList<>();
        int offset = 0;
        for (StructuredChunk chunk : chunks) {
            results.add(new TextChunker.ChunkResult(
                    results.size(),
                    chunk.content(),
                    offset,
                    offset + chunk.content().length()
            ));
            offset += chunk.content().length() + 2;
        }
        return applyOverlap(results, overlap);
    }

    private List<StructuredChunk> buildSemanticChunks(String text,
                                                      Function<List<String>, List<float[]>> embeddingProvider,
                                                      SemanticOptions semanticOptions) {
        List<String> blocks = splitSemanticSentenceGroups(text, embeddingProvider, semanticOptions);
        List<float[]> embeddings = embedOrNull(blocks, embeddingProvider);
        List<StructuredChunk> chunks = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            String block = blocks.get(i).trim();
            if (!block.isEmpty()) {
                chunks.add(new StructuredChunk(block, "SEMANTIC", block, embeddings == null ? null : embeddings.get(i)));
            }
        }
        return chunks;
    }

    private List<StructuredChunk> toStructuredChunks(List<String> blocks, String role) {
        List<StructuredChunk> chunks = new ArrayList<>();
        for (String block : blocks) {
            String trimmed = block.trim();
            if (!trimmed.isEmpty()) {
                chunks.add(new StructuredChunk(trimmed, role, trimmed));
            }
        }
        return chunks;
    }

    private List<TextChunker.ChunkResult> chunkStructured(List<String> blocks, int chunkSize, int overlap) {
        if (blocks.isEmpty()) {
            return List.of();
        }

        List<TextChunker.ChunkResult> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int startOffset = 0;
        int offset = 0;

        for (String block : blocks) {
            if (block.isBlank()) {
                continue;
            }

            if (block.length() > chunkSize) {
                appendChunk(chunks, current, startOffset);
                current = new StringBuilder();
                chunks.add(new TextChunker.ChunkResult(
                        chunks.size(),
                        block,
                        offset,
                        offset + block.length()
                ));
                offset += block.length() + 2;
                startOffset = offset;
                continue;
            }

            if (current.isEmpty()) {
                startOffset = offset;
            }

            if (!current.isEmpty() && current.length() + 2 + block.length() > chunkSize) {
                appendChunk(chunks, current, startOffset);
                current = new StringBuilder();
                startOffset = offset;
            }

            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(block);
            offset += block.length() + 2;
        }

        appendChunk(chunks, current, startOffset);
        return applyOverlap(chunks, overlap);
    }

    private List<String> splitParagraphBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        for (String paragraph : text.split("\n\n+")) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty()) {
                blocks.add(trimmed);
            }
        }
        return blocks;
    }

    private List<String> splitSemanticSentenceGroups(String text,
                                                     Function<List<String>, List<float[]>> embeddingProvider,
                                                     SemanticOptions options) {
        List<String> sentences = splitSentences(text);
        if (sentences.size() <= 1 || embeddingProvider == null) {
            return splitParagraphBlocks(text);
        }

        List<String> windows = buildSentenceWindows(sentences, options.windowSize());
        List<float[]> embeddings = embeddingProvider.apply(windows);
        if (embeddings == null || embeddings.size() != windows.size()) {
            return splitParagraphBlocks(text);
        }

        List<Double> similarities = new ArrayList<>();
        for (int i = 1; i < embeddings.size(); i++) {
            similarities.add(cosineSimilarity(embeddings.get(i - 1), embeddings.get(i)));
        }
        double threshold = resolveSemanticThreshold(similarities, options);

        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder(sentences.getFirst());
        for (int i = 1; i < sentences.size(); i++) {
            double similarity = similarities.get(i - 1);
            if (shouldSplitSemanticChunk(current, similarity, threshold, options)) {
                blocks.add(current.toString().trim());
                current = new StringBuilder(sentences.get(i));
            } else {
                current.append(" ").append(sentences.get(i));
            }
        }
        if (!current.isEmpty()) {
            blocks.add(current.toString().trim());
        }
        return normalizeBlocksBySize(blocks, options.maxChunkSize());
    }

    private List<String> normalizeBlocksBySize(List<String> blocks, int chunkSize) {
        List<String> normalized = new ArrayList<>();
        for (String block : blocks) {
            String trimmed = block == null ? "" : block.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() > chunkSize) {
                normalized.addAll(splitBySentenceLength(trimmed, chunkSize));
            } else {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (!current.isEmpty()) {
                current.append(" ");
            }
            current.append(line);
        }
        for (String sentence : current.toString().split("(?<=[.!?。！？])\\s+")) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    private List<String> splitBySentenceLength(String text, int chunkSize) {
        List<String> sentences = splitSentences(text);
        if (sentences.isEmpty()) {
            return splitByLength(text, chunkSize);
        }

        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (!current.isEmpty() && current.length() + 1 + sentence.length() > chunkSize) {
                blocks.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(sentence);
        }
        if (!current.isEmpty()) {
            blocks.add(current.toString().trim());
        }
        return blocks;
    }

    private List<String> buildSentenceWindows(List<String> sentences, int windowSize) {
        List<String> windows = new ArrayList<>();
        int size = Math.max(1, windowSize);
        for (int i = 0; i < sentences.size(); i++) {
            int from = Math.max(0, i - size + 1);
            StringBuilder window = new StringBuilder();
            for (int j = from; j <= i; j++) {
                if (!window.isEmpty()) {
                    window.append(' ');
                }
                window.append(sentences.get(j));
            }
            windows.add(window.toString());
        }
        return windows;
    }

    private boolean shouldSplitSemanticChunk(StringBuilder current,
                                             double similarity,
                                             double threshold,
                                             SemanticOptions options) {
        int minChunkSize = Math.max(0, options.minChunkSize());
        if (current.length() < minChunkSize) {
            return false;
        }
        return similarity < threshold;
    }

    private double resolveSemanticThreshold(List<Double> similarities, SemanticOptions options) {
        if (similarities.isEmpty()) {
            return options.thresholdValue();
        }
        String strategy = normalize(options.thresholdStrategy(), "FIXED");
        return switch (strategy) {
            case "PERCENTILE" -> percentile(similarities, options.thresholdValue());
            case "STDDEV" -> mean(similarities) - options.thresholdValue() * stddev(similarities);
            case "IQR" -> {
                double q1 = percentile(similarities, 0.25d);
                double q3 = percentile(similarities, 0.75d);
                yield q1 - options.thresholdValue() * (q3 - q1);
            }
            default -> options.thresholdValue();
        };
    }

    private double mean(List<Double> values) {
        double sum = 0d;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private double stddev(List<Double> values) {
        double mean = mean(values);
        double sum = 0d;
        for (double value : values) {
            double delta = value - mean;
            sum += delta * delta;
        }
        return Math.sqrt(sum / values.size());
    }

    private double percentile(List<Double> values, double ratio) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        double bounded = Math.max(0d, Math.min(1d, ratio));
        int index = (int) Math.floor((sorted.size() - 1) * bounded);
        return sorted.get(index);
    }

    private List<String> splitByLength(String text, int chunkSize) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!current.isEmpty()
                    && current.length() + 1 + trimmed.length() > chunkSize
                    && !isStandaloneHeadingBlock(current.toString())) {
                blocks.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append("\n");
            }
            current.append(trimmed);
        }
        if (!current.isEmpty()) {
            blocks.add(current.toString().trim());
        }
        return blocks;
    }

    private void appendChunk(List<TextChunker.ChunkResult> chunks, StringBuilder content, int startOffset) {
        if (content.isEmpty()) {
            return;
        }
        String text = content.toString().trim();
        chunks.add(new TextChunker.ChunkResult(
                chunks.size(),
                text,
                startOffset,
                startOffset + text.length()
        ));
    }

    private List<String> splitMarkdownBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : text.split("\n\n+")) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (isHeading(trimmed) && !current.isEmpty()) {
                blocks.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(trimmed);
        }

        if (!current.isEmpty()) {
            blocks.add(current.toString().trim());
        }
        return blocks;
    }

    private List<String> splitStructuredBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                if (!isHeading(current.toString().trim())) {
                    flushBlock(blocks, current);
                }
                continue;
            }

            if (isHeading(line)) {
                flushBlock(blocks, current);
                current.append(line);
                continue;
            }

            if (isTableLine(line)) {
                String currentText = current.toString().trim();
                if (isHeading(currentText) && !currentText.contains("|")) {
                    current.append("\n").append(line);
                    continue;
                }

                if (isHeading(currentText) && isTableSeparator(line)) {
                    current.append("\n").append(line);
                    continue;
                }

                if (isHeading(currentText) && currentText.contains("|")) {
                    flushBlock(blocks, current);
                    current.append(line);
                    continue;
                }

                if (currentText.contains("|") && !isTableSeparator(line)) {
                    flushBlock(blocks, current);
                    current.append(line);
                    continue;
                }

                if (!currentText.isEmpty() && !currentText.contains("|")) {
                    flushBlock(blocks, current);
                }

                if (!current.isEmpty()) {
                    current.append("\n");
                }
                current.append(line);
                continue;
            }

            flushBlock(blocks, current);
            current.append(line);
        }

        flushBlock(blocks, current);
        return blocks;
    }

    private List<String> splitByDelimiters(String text, List<String> delimiters) {
        if (delimiters == null || delimiters.isEmpty()) {
            return splitStructuredBlocks(text);
        }

        String normalized = text;
        for (String delimiter : delimiters) {
            normalized = normalized.replace(delimiter, "\u0000");
        }

        List<String> blocks = new ArrayList<>();
        for (String part : normalized.split("\u0000")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                blocks.add(trimmed);
            }
        }
        return blocks;
    }

    private void flushBlock(List<String> blocks, StringBuilder current) {
        if (!current.isEmpty()) {
            blocks.add(current.toString().trim());
            current.setLength(0);
        }
    }

    private boolean isStructuredFile(String fileName, String parserType) {
        String parser = normalize(parserType, "AUTO");
        if ("STRUCTURED".equals(parser) || "MARKDOWN".equals(parser)) {
            return true;
        }
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".xlsx")
                || lower.endsWith(".xls")
                || lower.endsWith(".csv")
                || lower.endsWith(".ppt")
                || lower.endsWith(".pptx")
                || lower.endsWith(".pdf")
                || lower.endsWith(".doc")
                || lower.endsWith(".docx")
                || lower.endsWith(".md");
    }

    private boolean isMarkdownFile(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".md") || lower.endsWith(".markdown");
    }

    private boolean isHeading(String line) {
        return line.startsWith("#");
    }

    private boolean isTableLine(String line) {
        return line.contains("|") || line.startsWith("【表格】") || line.startsWith("【表格结束】");
    }

    private boolean isTableSeparator(String line) {
        return line.replace("|", "").replace("-", "").trim().isEmpty() && line.contains("-");
    }

    private boolean isStandaloneHeadingBlock(String block) {
        String trimmed = block == null ? "" : block.trim();
        return isHeading(trimmed) && !trimmed.contains("\n");
    }

    private List<float[]> embedOrNull(List<String> texts,
                                      Function<List<String>, List<float[]>> embeddingProvider) {
        if (embeddingProvider == null || texts.isEmpty()) {
            return null;
        }
        return embeddingProvider.apply(texts);
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || left.length != right.length) {
            return 1d;
        }
        double dot = 0d;
        double leftNorm = 0d;
        double rightNorm = 0d;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0d || rightNorm == 0d) {
            return 1d;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private String normalize(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase(Locale.ROOT);
    }

    private List<TextChunker.ChunkResult> applyOverlap(List<TextChunker.ChunkResult> chunks, int overlap) {
        if (overlap <= 0 || chunks.size() <= 1) {
            return chunks;
        }

        List<TextChunker.ChunkResult> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            StringBuilder content = new StringBuilder();
            for (int j = Math.max(0, i - overlap); j < i; j++) {
                content.append(chunks.get(j).content()).append("\n\n");
            }
            content.append(chunks.get(i).content());
            result.add(new TextChunker.ChunkResult(
                    chunks.get(i).index(),
                    content.toString().trim(),
                    chunks.get(i).startOffset(),
                    chunks.get(i).endOffset()
            ));
        }
        return result;
    }
}
