package com.hxh.apboa.security.script;

import com.hxh.apboa.security.script.model.ScriptType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * 描述：检查器注册中心，维护 ScriptType → ScriptSecurityChecker 的映射。
 * 内置默认检查器，也支持用户注册自定义实现来覆盖或扩展。
 *
 * @author huxuehao
 */
public class CheckerRegistry {

    /** ScriptType → Checker 的映射表 */
    private final Map<ScriptType, ScriptSecurityChecker> registry = new EnumMap<>(ScriptType.class);

    /**
     * 注册一个检查器（如果该类型已存在则覆盖）
     *
     * @param checker 检查器实现
     */
    public void register(ScriptSecurityChecker checker) {
        registry.put(checker.supportedType(), checker);
    }

    /**
     * 注册指定类型的检查器
     *
     * @param type    脚本类型
     * @param checker 检查器实现
     */
    public void register(ScriptType type, ScriptSecurityChecker checker) {
        registry.put(type, checker);
    }

    /**
     * 获取指定类型的检查器
     *
     * @param type 脚本类型
     * @return 检查器，未注册时返回 Optional.empty()
     */
    public Optional<ScriptSecurityChecker> get(ScriptType type) {
        return Optional.ofNullable(registry.get(type));
    }

    /**
     * 检查指定类型是否已注册检查器
     */
    public boolean hasChecker(ScriptType type) {
        return registry.containsKey(type);
    }

    /**
     * 获取已注册的类型数量
     */
    public int getRegisteredCount() {
        return registry.size();
    }

}
