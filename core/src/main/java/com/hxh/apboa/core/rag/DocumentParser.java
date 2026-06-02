package com.hxh.apboa.core.rag;

import com.hxh.apboa.core.rag.parser.impl.ExcelParser;
import com.hxh.apboa.core.rag.parser.impl.PptParser;
import com.hxh.apboa.core.rag.parser.impl.TikaParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * 文档解析入口，根据文件类型路由到专用解析器，并在配置启用时追加多模态解析结果。
 *
 * @author huxuehao
 */
@Component
public class DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(DocumentParser.class);

    private static final Set<String> EXCEL_TYPES = Set.of("xlsx", "xls", "csv");
    private static final Set<String> PPT_TYPES = Set.of("pptx", "ppt");
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "txt", "md", "pdf", "doc", "docx", "xlsx", "xls", "csv", "pptx", "ppt"
    );

    private final PptParser pptParser = new PptParser();
    private final TikaParser tikaParser = new TikaParser();
    private final MultimodalDocumentParser multimodalDocumentParser;

    public DocumentParser() {
        this.multimodalDocumentParser = null;
    }

    @Autowired
    public DocumentParser(ObjectProvider<MultimodalDocumentParser> multimodalDocumentParserProvider) {
        this.multimodalDocumentParser = multimodalDocumentParserProvider.getIfAvailable();
    }

    /**
     * 判断文件类型是否受支持。
     */
    public boolean isNotSupported(String fileName) {
        String ext = extractExtension(fileName);
        return ext == null || !SUPPORTED_TYPES.contains(ext);
    }

    /**
     * 解析文档输入流，提取可用于 RAG 的文本内容。
     *
     * @param inputStream 文档输入流
     * @param fileName    文件名
     * @return 解析后的文本内容
     */
    public String parse(InputStream inputStream, String fileName) {
        return parse(inputStream, fileName, null);
    }

    /**
     * 解析文档输入流，提取可用于 RAG 的文本内容。
     *
     * @param inputStream  文档输入流
     * @param fileName     文件名
     * @param rowDelimiter 行分隔符，用于 Excel 等表格文档
     * @return 解析后的文本内容
     */
    public String parse(InputStream inputStream, String fileName, String rowDelimiter) {
        byte[] fileBytes = readAllBytes(inputStream, fileName);
        String ext = extractExtension(fileName);
        if (ext == null || !SUPPORTED_TYPES.contains(ext)) {
            throw new RuntimeException("不支持的文件类型: " + fileName);
        }

        try {
            String text;
            if (EXCEL_TYPES.contains(ext)) {
                ExcelParser excelParser = new ExcelParser();
                excelParser.setRowDelimiter(rowDelimiter);
                text = excelParser.parse(new ByteArrayInputStream(fileBytes), fileName,
                        (fallbackStream, fallbackFileName) -> tikaParser.parse(
                                new ByteArrayInputStream(fileBytes), fallbackFileName, null));
            } else if (PPT_TYPES.contains(ext)) {
                text = pptParser.parse(new ByteArrayInputStream(fileBytes), fileName,
                        (fallbackStream, fallbackFileName) -> tikaParser.parse(
                                new ByteArrayInputStream(fileBytes), fallbackFileName, null));
            } else {
                text = tikaParser.parse(new ByteArrayInputStream(fileBytes), fileName, null);
            }
            return appendMultimodalContent(fileBytes, fileName, text);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析失败, fileName={}", fileName, e);
            throw new RuntimeException("文档解析失败: " + fileName, e);
        }
    }

    private byte[] readAllBytes(InputStream inputStream, String fileName) {
        try {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("读取文档失败: " + fileName, e);
        }
    }

    private String appendMultimodalContent(byte[] fileBytes, String fileName, String text) {
        if (multimodalDocumentParser == null || !multimodalDocumentParser.isEnabled()) {
            return text;
        }
        String multimodalText = multimodalDocumentParser.parse(fileBytes, fileName);
        if (multimodalText == null || multimodalText.isBlank()) {
            return text;
        }
        if (text == null || text.isBlank()) {
            return multimodalText;
        }
        return text + "\n\n---\n\n" + multimodalText;
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
