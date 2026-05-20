package com.hxh.apboa.security.script;

import com.hxh.apboa.security.script.model.ScriptType;
import com.hxh.apboa.security.script.model.SecurityFinding;

import java.util.List;

/**
 * 描述：脚本安全检查器接口，每种脚本语言对应一个实现。
 * 第三方开发者实现此接口并注册到 CheckerRegistry 即可扩展新的脚本语言检查能力。
 *
 * @author huxuehao
 */
public interface ScriptSecurityChecker {

    /**
     * 返回此检查器支持的脚本类型
     *
     * @return 支持的脚本类型
     */
    ScriptType supportedType();

    /**
     * 对脚本内容执行安全检查
     *
     * @param content 脚本的完整文本内容
     * @return 发现的所有不安全行为列表，安全时返回空列表
     */
    List<SecurityFinding> check(String content);

}
