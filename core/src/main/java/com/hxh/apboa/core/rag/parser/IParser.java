package com.hxh.apboa.core.rag.parser;

import java.io.InputStream;

/**
 * 描述：文档解析器接口，定义文档解析的抽象契约
 *
 * @author huxuehao
 **/
public interface IParser {

    /**
     * 降级解析方案（函数式接口）
     * 当专用解析器解析失败时，可调用此降级方案进行兜底解析
     */
    @FunctionalInterface
    interface ParseFallback {
        /**
         * 降级解析
         *
         * @param inputStream 文档输入流
         * @param fileName    文件名
         * @return 解析后的文本内容
         * @throws Exception 解析异常
         */
        String fallback(InputStream inputStream, String fileName) throws Exception;
    }

    /**
     * 解析文档（无降级方案）
     *
     * @param inputStream 文档输入流
     * @param fileName    文件名
     * @return 解析后的文本内容
     */
    default String parse(InputStream inputStream, String fileName) {
        return parse(inputStream, fileName, null);
    }

    /**
     * 解析文档（支持降级方案）
     *
     * @param inputStream 文档输入流
     * @param fileName    文件名
     * @param fallback    降级解析方案，当主解析失败时调用，可为 null
     * @return 解析后的文本内容
     */
    String parse(InputStream inputStream, String fileName, ParseFallback fallback);

    /**
     * 清理文档提取的原始内容
     *
     * @param content 原始文本内容
     * @return 清洗后的文本
     */
    default String cleanContent(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        // 1. 统一换行符
        String cleaned = content.replace("\r\n", "\n").replace("\r", "\n");

        // 2. 合并多个连续空行为单个空行（保留段落结构）
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");

        // 3. 去除每行首尾的空白字符（保留行内空格）
        String[] lines = cleaned.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line.trim()).append("\n");
        }
        cleaned = sb.toString();

        // 4. 去除不可见控制字符（保留常用空白字符）
        cleaned = cleaned.replaceAll("\\p{Cntrl}&&[^\\n\\t]", "");

        // 5. 规范空白字符（连续空格合并为单个空格，但不影响表格格式）
        cleaned = cleaned.replaceAll(" {2,}", " ");

        // 6. 去除首尾空白
        cleaned = cleaned.trim();

        // 可选：去除特定的噪音字符
        cleaned = cleaned.replaceAll("[\\u00A0\\u2007\\u202F]", " "); // 特殊空格转普通空格
        cleaned = cleaned.replaceAll("\\uFFFD", ""); // 去除无效Unicode字符

        return cleaned;
    }
}
