package com.hxh.apboa.core.rag.parser.impl;

import com.hxh.apboa.core.rag.parser.IParser;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * 描述：通用文档解析器，基于Apache Tika支持多种格式（txt、md、pdf、doc、docx等）
 *
 * @author huxuehao
 **/
public class TikaParser implements IParser {
    private static final Logger log = LoggerFactory.getLogger(TikaParser.class);

    /**
     * 内存写入阈值：字符数超过此值后，Tika自动切换为临时文件写入，不会截断内容。
     * 设置为 -1 表示全内存写入（有OOM风险），此处设 50MB 作为安全水位线，
     * 图片较多的文档虽然输出字符量大，但超出部分会落盘，不会丢失文本。
     */
    private static final int WRITE_LIMIT = 50 * 1024 * 1024;

    @Override
    public String parse(InputStream inputStream, String fileName, ParseFallback fallback) {
        try {
            return doParse(inputStream, fileName);
        } catch (Exception e) {
            log.warn("Tika通用解析失败，尝试降级方案, fileName={}", fileName, e);
            if (fallback != null) {
                try {
                    return fallback.fallback(inputStream, fileName);
                } catch (Exception ex) {
                    throw new RuntimeException("文档解析失败: " + fileName, ex);
                }
            }
            throw new RuntimeException("文档解析失败: " + fileName, e);
        }
    }

    /**
     * 使用Tika自动检测格式并提取内容，同时提取元数据作为上下文
     *
     * @param inputStream 文档输入流
     * @param fileName    文件名
     * @return 解析后的结构化文本
     */
    private String doParse(InputStream inputStream, String fileName) throws Exception {
        Parser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(WRITE_LIMIT);
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        ParseContext context = new ParseContext();
        context.set(Parser.class, parser);

        parser.parse(inputStream, handler, metadata, context);

        String content = handler.toString();
        if (content == null || content.isBlank()) {
            log.warn("文档解析结果为空, fileName={}", fileName);
            return "";
        }

        // 构建带元数据上下文的结构化输出
        String metadataContext = buildMetadataContext(metadata);
        if (!metadataContext.isEmpty()) {
            content = metadataContext + "\n---\n\n" + content;
        }

        return cleanContent(content);
    }

    /**
     * 构建文档元数据上下文，提取标题、作者等信息以增强RAG检索精度
     *
     * @param metadata Tika元数据
     * @return 结构化的元数据文本
     */
    private String buildMetadataContext(Metadata metadata) {
        StringBuilder sb = new StringBuilder();

        String title = metadata.get(TikaCoreProperties.TITLE);
        if (title != null && !title.isBlank()) {
            sb.append("标题：").append(title.trim()).append("\n");
        }

        String author = metadata.get(TikaCoreProperties.CREATOR);
        if (author != null && !author.isBlank()) {
            sb.append("作者：").append(author.trim()).append("\n");
        }

        String date = metadata.get(TikaCoreProperties.CREATED);
        if (date == null || date.isBlank()) {
            date = metadata.get(TikaCoreProperties.MODIFIED);
        }
        if (date != null && !date.isBlank()) {
            sb.append("日期：").append(date.trim()).append("\n");
        }

        String contentType = metadata.get(Metadata.CONTENT_TYPE);
        if (contentType != null && !contentType.isBlank()) {
            sb.append("类型：").append(contentType.trim()).append("\n");
        }

        return sb.toString().trim();
    }
}
