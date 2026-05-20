package com.hxh.apboa.security.script.model;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 描述：脚本类型枚举，支持通过文件名后缀自动识别脚本类型
 *
 * @author huxuehao
 */
public enum ScriptType {

    /** Shell脚本 — .sh .bash .zsh .ksh .fish */
    SHELL("shell", Set.of(".sh", ".bash", ".zsh", ".ksh", ".fish")),

    /** Python脚本 — .py .py3 .pyw .ipy .pyx */
    PYTHON("python", Set.of(".py", ".py3", ".pyw", ".ipy", ".pyx")),

    /** Node.js脚本 — .js .mjs .cjs .jsx .ts .tsx */
    NODEJS("nodejs", Set.of(".js", ".mjs", ".cjs", ".jsx", ".ts", ".tsx")),

    /** HTML文件 — .html .htm .xhtml .shtml .svg */
    HTML("html", Set.of(".html", ".htm", ".xhtml", ".shtml", ".svg"));

    private final String typeName;
    private final Set<String> extensions;

    ScriptType(String typeName, Set<String> extensions) {
        this.typeName = typeName;
        this.extensions = extensions;
    }

    public String getTypeName() {
        return typeName;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    /**
     * 根据文件名自动识别脚本类型
     *
     * @param fileName 文件名（如 "install.sh"）
     * @return 识别到的脚本类型，识别失败返回 Optional.empty()
     */
    public static Optional<ScriptType> fromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        String lowerName = fileName.toLowerCase();
        for (ScriptType type : values()) {
            for (String ext : type.extensions) {
                if (lowerName.endsWith(ext)) {
                    return Optional.of(type);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 根据脚本类型名查找
     *
     * @param typeName 类型名（如 "shell", "python"）
     * @return 对应的 ScriptType，未找到返回 Optional.empty()
     */
    public static Optional<ScriptType> fromTypeName(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return Optional.empty();
        }
        for (ScriptType type : values()) {
            if (type.typeName.equalsIgnoreCase(typeName)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

}
