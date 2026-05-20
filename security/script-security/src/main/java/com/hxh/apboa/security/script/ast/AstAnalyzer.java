package com.hxh.apboa.security.script.ast;

import com.hxh.apboa.security.script.model.SecurityFinding;

import java.util.List;

/**
 * 描述：AST 深度分析器接口，用于在正则层之外做语义级别的安全检查。
 * 每种脚本语言可对应一个 AST 分析器实现，进行变量流向追踪、
 * 函数参数语义分析、代码混淆还原等高级检测。
 *
 * @author huxuehao
 */
public interface AstAnalyzer {

    /**
     * 对脚本内容执行AST深度语义分析
     *
     * @param content 脚本完整文本内容
     * @return 发现的不安全行为列表
     */
    List<SecurityFinding> analyze(String content);

    /**
     * 返回此分析器支持的脚本类型名称
     *
     * @return 脚本类型名，如 "python"、"nodejs"
     */
    String supportedType();

}
