package com.hxh.apboa.common.exception;

import com.hxh.apboa.common.r.R;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 描述：全局异常处理器
 *
 * @author huxuehao
 **/
@Slf4j
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    // 索引名到中文描述的映射缓存
    private static final Map<String, String> INDEX_FIELD_MAPPING = new HashMap<>();

    static {
        // 初始化常见的索引映射
        INDEX_FIELD_MAPPING.put("model_provider.uk_provider_name", "提供商名称");
        INDEX_FIELD_MAPPING.put("model_config.uk_model_provider_id", "模型名称");
        INDEX_FIELD_MAPPING.put("system_prompt_template.uk_template_name_category", "模板名称");
        INDEX_FIELD_MAPPING.put("sensitive_word_config.uk_sw_name_category", "配置名称");
        INDEX_FIELD_MAPPING.put("tool_config.uk_tool_id", "工具编号");
        INDEX_FIELD_MAPPING.put("mcp_server.uk_mcp_name", "MCP名称");
        INDEX_FIELD_MAPPING.put("skill_package.uk_skill_name", "Skill名称");
        INDEX_FIELD_MAPPING.put("knowledge_base_config.uk_kb_name", "知识库名称");
        INDEX_FIELD_MAPPING.put("agent_definition.uk_agent_code", "智能体编号");
        INDEX_FIELD_MAPPING.put("agent_definition.uk_agent_name", "智能体名称");
        INDEX_FIELD_MAPPING.put("account.uk_email", "邮箱");
        INDEX_FIELD_MAPPING.put("account.uk_username", "用户名");
        INDEX_FIELD_MAPPING.put("account_role.uk_account_role", "账号角色");
        INDEX_FIELD_MAPPING.put("agent_hooks.uk_agent_hook", "智能体Hook");
        INDEX_FIELD_MAPPING.put("agent_knowledge_bases.uk_agent_kb", "智能体知识库");
        INDEX_FIELD_MAPPING.put("agent_mcp_servers.uk_agent_mcp", "智能体MCP");
        INDEX_FIELD_MAPPING.put("agent_skill_packages.uk_agent_skill", "智能体Kill");
        INDEX_FIELD_MAPPING.put("agent_sub_agents.uk_parent_sub_agent", "智能体AgentAsTool");
    }

    // ================ MyBatis-Plus 相关异常处理 ================

    /**
     * 专门处理MyBatis-Plus的TooManyResultsException
     * getOne/selectOne方法返回多条记录时抛出
     */
    @ExceptionHandler(value = {TooManyResultsException.class})
    public R<?> tooManyResultsExceptionHandler(TooManyResultsException e) {
        log.warn("查询到多条记录异常: {}", e.getMessage());

        // 提取查询相关参数信息
        String queryInfo = extractQueryInfo(e);
        log.debug("查询条件详情: {}", queryInfo);

        // 分析异常原因，提供精准提示
        String userFriendlyMsg = analyzeTooManyResultsError(e);

        return R.fail(400, userFriendlyMsg);
    }

    /**
     * 处理MyBatisPlusException及其子类
     */
    @ExceptionHandler(value = {MybatisPlusException.class})
    public R<?> mybatisPlusExceptionHandler(MybatisPlusException e) {
        log.error("MyBatis-Plus操作异常", e);

        String errorMsg = extractMyBatisPlusError(e);
        return R.fail(500, errorMsg);
    }

    // ================ 数据库相关异常处理 ================

    /**
     * 唯一索引/主键冲突异常处理
     */
    @ExceptionHandler(value = {DuplicateKeyException.class})
    public R<?> duplicateKeyExceptionHandler(DuplicateKeyException e) {
        log.warn("数据重复异常: {}", e.getMessage());

        // 从异常信息中提取关键信息
        String message = e.getMessage();
        String userFriendlyMsg = extractUserFriendlyMessage(message);

        // 记录详细日志但不暴露给前端
        log.debug("完整异常信息: ", e);

        return R.fail(400, userFriendlyMsg);
    }

    /**
     * 数据库约束异常（包括外键、非空等）
     */
    @ExceptionHandler(value = {DataIntegrityViolationException.class})
    public R<?> dataIntegrityViolationHandler(DataIntegrityViolationException e) {
        log.warn("数据完整性异常: {}", e.getMessage());

        // 判断是否为外键约束
        if (e.getMessage().contains("foreign key constraint")) {
            return R.fail(400, "关联数据不存在，请检查关联信息");
        }

        // 判断是否为空约束
        if (e.getMessage().contains("cannot be null") || e.getMessage().contains("not null")) {
            return R.fail(400, "必填字段不能为空");
        }

        // 判断长度约束
        if (e.getMessage().contains("too long") || e.getMessage().contains("value too large")) {
            return R.fail(400, "字段长度超过限制");
        }

        return R.fail(400, "数据校验失败，请检查输入数据");
    }

    /**
     * 数据库异常统一处理
     */
    @ExceptionHandler(value = {SQLException.class})
    public R<?> sqlExceptionHandler(SQLException e) {
        log.error("数据库操作异常，错误码: {}，SQL状态: {}", e.getErrorCode(), e.getSQLState(), e);

        // 根据SQL状态码判断
        switch (e.getSQLState()) {
            case "23000": // 完整性约束违反
                return R.fail(400, "数据约束冲突，请检查输入数据");
            case "28000": // 无效的授权
                return R.fail(500, "数据库连接异常");
            case "HY000": // 一般错误
                if (e.getMessage().contains("lock") || e.getMessage().contains("deadlock")) {
                    return R.fail(409, "数据正在被其他操作占用，请稍后重试");
                }
            case "08001": // 无法建立连接
            case "08004": // 连接被拒绝
                return R.fail(500, "数据库连接失败");
        }

        return R.fail(500, "数据库操作失败，请稍后重试");
    }

    /**
     * MyBatis特定异常
     */
    @ExceptionHandler(value = {PersistenceException.class})
    public R<?> persistenceExceptionHandler(PersistenceException e) {
        log.error("持久化操作异常", e);

        Throwable cause = e.getCause();

        // 优先处理TooManyResultsException
        if (cause instanceof TooManyResultsException) {
            return tooManyResultsExceptionHandler((TooManyResultsException) cause);
        }

        // 处理SQL异常
        if (cause instanceof SQLException) {
            return sqlExceptionHandler((SQLException) cause);
        }

        // 处理重复键异常
        if (cause instanceof DuplicateKeyException) {
            return duplicateKeyExceptionHandler((DuplicateKeyException) cause);
        }

        return R.fail(500, "系统内部异常");
    }

    // ================ 业务相关异常处理 ================

    @ExceptionHandler(NotAuthException.class)
    public ResponseEntity<?> notAuthExceptionHandler(NotAuthException e) {
        log.warn("权限验证失败: {}", e.getMessage());
        Throwable cause = e.getCause();
        String message = cause != null ? cause.getMessage() : e.getMessage();
        return new ResponseEntity<>(R.fail(401, message), HttpStatus.UNAUTHORIZED);
    }

    /**
     * 业务异常处理
     */
    @ExceptionHandler(value = {BusinessException.class})
    public R<?> businessExceptionHandler(BusinessException e) {
        log.warn("业务异常[{}]: {}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public R<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        log.warn("参数校验失败: {}", e.getMessage());

        StringBuilder errorMsg = new StringBuilder();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errorMsg.append(fieldError.getField())
                    .append(": ")
                    .append(fieldError.getDefaultMessage())
                    .append("; ");
        }

        return R.fail(400, errorMsg.toString());
    }

    // ================ 通用异常处理 ================

    @ExceptionHandler(Exception.class)
    public R<?> exceptionHandler(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);

        String errorMsg = "系统异常，请稍后重试";
        if (e.getMessage() != null) {
            errorMsg = e.getMessage();
        }

        return R.fail(500, errorMsg);
    }

    // ================ 私有辅助方法 ================

    /**
     * 从数据库异常信息中提取用户友好信息
     */
    private String extractUserFriendlyMessage(String originalMsg) {
        if (originalMsg == null) {
            return "数据已存在，请勿重复添加";
        }

        // 根据不同的数据库异常信息进行提取
        String lowerMsg = originalMsg.toLowerCase();

        if (lowerMsg.contains("duplicate entry")) {
            // MySQL格式: Duplicate entry 'xxx' for key 'uk_name'
            return extractDuplicateInfo(originalMsg);
        } else if (lowerMsg.contains("unique constraint") || lowerMsg.contains("duplicate key")) {
            return extractDuplicateInfo(originalMsg);
        } else if (lowerMsg.contains("违反唯一键约束") || lowerMsg.contains("唯一键冲突")) {
            // 中文数据库错误信息
            return "数据已存在，请勿重复添加";
        } else if (lowerMsg.contains("uk_") || lowerMsg.contains("pk_") || lowerMsg.contains("idx_")) {
            // 包含索引名的情况
            return "数据已存在，请检查相关字段";
        }

        return "数据已存在，请勿重复添加";
    }

    /**
     * 提取重复字段的具体信息
     */
    private String extractDuplicateInfo(String msg) {
        try {
            // 尝试匹配MySQL的重复条目信息
            Pattern pattern = Pattern.compile("Duplicate entry '(.+?)' for key '(.+?)'");
            Matcher matcher = pattern.matcher(msg);
            if (matcher.find()) {
                String duplicateValue = matcher.group(1);
                String indexName = matcher.group(2);

                // 根据索引名映射到具体字段
                String fieldName = mapIndexToField(indexName);

                return String.format("%s [%s] 已存在，请使用其他值", fieldName, duplicateValue);
            }

            // 尝试匹配其他格式
            pattern = Pattern.compile("unique constraint \\((.+?)\\)");
            matcher = pattern.matcher(msg);
            if (matcher.find()) {
                String constraintName = matcher.group(1);
                return String.format("数据唯一性冲突，约束名: %s", constraintName);
            }
        } catch (Exception e) {
            log.debug("解析重复信息失败", e);
        }

        return "数据已存在，请检查相关字段";
    }

    /**
     * 索引名到字段名的映射
     */
    private String mapIndexToField(String indexName) {
        // 先从预定义的映射中查找
        String fieldName = INDEX_FIELD_MAPPING.get(indexName);
        if (fieldName != null) {
            return fieldName;
        }

        return "表单值";
    }

    /**
     * 分析TooManyResultsException的具体原因
     */
    private String analyzeTooManyResultsError(TooManyResultsException e) {
        String originalMsg = e.getMessage();

        if (originalMsg == null || originalMsg.trim().isEmpty()) {
            return "查询条件不准确，返回了多条结果";
        }

        String lowerMsg = originalMsg.toLowerCase();

        if (lowerMsg.contains("expected one result") ||
                lowerMsg.contains("too many results")) {
            return "查询条件不精确，匹配到多条数据，请提供更精确的查询条件";
        }

        if (lowerMsg.contains("selectone") || lowerMsg.contains("getone")) {
            return "使用getOne/selectOne查询时匹配到多条记录，建议使用list查询或添加更精确的条件";
        }

        return "数据查询异常：查询条件匹配不唯一";
    }

    /**
     * 提取查询相关信息（用于日志记录）
     */
    private String extractQueryInfo(TooManyResultsException e) {
        try {
            // 可以尝试从堆栈信息中提取查询相关的类和方法
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                if (element.getClassName().contains("mapper") ||
                        element.getClassName().contains("service")) {
                    return String.format("类: %s, 方法: %s, 行号: %d",
                            element.getClassName(),
                            element.getMethodName(),
                            element.getLineNumber());
                }
            }
        } catch (Exception ex) {
            log.debug("提取查询信息失败", ex);
        }
        return "查询信息提取失败";
    }

    /**
     * 提取MyBatis-Plus特定的错误信息
     */
    private String extractMyBatisPlusError(MybatisPlusException e) {
        String msg = e.getMessage();

        if (msg == null) {
            return "数据操作失败";
        }

        if (msg.contains("wrapper must not be null")) {
            return "查询条件不能为空";
        }

        if (msg.contains("entity must not be null")) {
            return "操作对象不能为空";
        }

        if (msg.contains("id must not be null")) {
            return "ID不能为空";
        }

        if (msg.contains("can not find lambda cache")) {
            return "查询条件构建失败";
        }

        return "数据操作失败，请检查输入参数";
    }
}
