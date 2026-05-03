package com.hxh.apboa.core.workspace.hook;

/**
 * 描述：工作空间安全异常，所有路径/命令验证失败时抛出此异常
 *
 * @author huxuehao
 **/
public class WorkspaceSecurityException extends RuntimeException {

    private static final String HINT_SUFFIX = " 请阅读并遵守技能 'workspace_path_and_execution_rules'。";

    public WorkspaceSecurityException(String message) {
        super(message + HINT_SUFFIX);
    }
}
