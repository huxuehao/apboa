package com.hxh.apboa.core.rag;

import com.hxh.apboa.core.rag.parser.impl.ExcelParser;
import com.hxh.apboa.core.rag.parser.impl.PptParser;
import com.hxh.apboa.core.rag.parser.impl.TikaParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Set;

/**
 * 描述：文档解析器入口，根据文件类型路由到对应的专用解析器
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
    private final ExcelParser excelParser = new ExcelParser();
    private final TikaParser tikaParser = new TikaParser();

    /**
     * 判断文件类型是否受支持
     */
    public boolean isNotSupported(String fileName) {
        String ext = extractExtension(fileName);
        return ext == null || !SUPPORTED_TYPES.contains(ext);
    }

    /**
     * 解析文档输入流，提取纯文本内容
     *
     * @param inputStream 文档输入流
     * @param fileName    文件名（用于日志和类型推断）
     * @return 解析后的文本内容
     */
    public String parse(InputStream inputStream, String fileName) {
        return parse(inputStream, fileName, null);
    }

    /**
     * 解析文档输入流，提取纯文本内容
     *
     * @param inputStream   文档输入流
     * @param fileName      文件名（用于日志和类型推断）
     * @param rowDelimiter  行分隔符，用于 Excel 等表格文档，在每行后追加此分隔符以便后续分块
     * @return 解析后的文本内容
     */
    public String parse(InputStream inputStream, String fileName, String rowDelimiter) {
        String ext = extractExtension(fileName);
        if (ext == null || !SUPPORTED_TYPES.contains(ext)) {
            throw new RuntimeException("不支持的文件类型: " + fileName);
        }

        try {
            if (EXCEL_TYPES.contains(ext)) {
                excelParser.setRowDelimiter(rowDelimiter);
                return excelParser.parse(inputStream, fileName, tikaParser::parse);
            } else if (PPT_TYPES.contains(ext)) {
                return pptParser.parse(inputStream, fileName, tikaParser::parse);
            } else {
                return tikaParser.parse(inputStream, fileName, null);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析失败, fileName={}", fileName, e);
            throw new RuntimeException("文档解析失败: " + fileName, e);
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
