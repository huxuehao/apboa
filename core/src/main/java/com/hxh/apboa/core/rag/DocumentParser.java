package com.hxh.apboa.core.rag;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 文档解析器，基于Apache Tika支持多种文档格式
 *
 * @author huxuehao
 */
@Component
public class DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(DocumentParser.class);

    /**
     * 解析文档输入流，提取纯文本内容
     *
     * @param inputStream 文档输入流
     * @param fileName    文件名（用于日志和类型推断）
     * @return 解析后的文本内容
     */
    public String parse(InputStream inputStream, String fileName) {
        try {
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
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

            return cleanContent(content);
        } catch (Exception e) {
            log.error("文档解析失败, fileName={}", fileName, e);
            throw new RuntimeException("文档解析失败: " + fileName, e);
        }
    }

    /**
     * 清洗文本内容：去除多余空白、特殊字符
     */
    private String cleanContent(String content) {
        return content
                .replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .replaceAll("[ \t]+", " ")
                .trim();
    }
}
