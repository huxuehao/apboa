package com.hxh.apboa.common.consts;

import com.hxh.apboa.common.util.CryptoUtils;

import java.util.List;

/**
 * 描述：系统常量
 *
 * @author huxuehao
 **/
public class SysConst {
    /**
     * JWT密钥
     */
    public static final String JWT_SECRET_KEY = "jwt.secret";
    /**
     * 登录用户Key
     */
    public static final String LOGIN_USER_KEY = "LOGIN-USER-KEY";
    public static final String USER_DETAIL = "USER-DETAIL";

    /**
     * token过期时间（6小时）
     */
    public static final Long ACCESS_TOKEN_TTL = 1000 * 60 * 60 * 6L;

    /**
     * refresh token过期时间（12小时）
     */
    public static final Long REFRESH_TOKEN_TTL = 1000 * 60 * 60 * 18L;

    /**
     * 单个文件的最大体积（单位：MB）
     */
    public static final String SINGLE_FILE_MAX_SIZE = "5";

    public static final String ALLOW_IMAGE_FILE_TYPE = "png,jpeg,png,gif,webp";
    public static final String ALLOW_AUDIO_FILE_TYPE = "mp3,wav,mpeg";
    public static final String ALLOW_VIDEO_FILE_TYPE = "mp4,mpeg";

    public static final Long ADMIN_ACCOUNT_ID = 1111111111111111111L;

    public static final String CURRENT_NODE_ID = CryptoUtils.uuid();

    public static final String CHAT_KEY_TO_AGENT_CODE_PREFIX = "apboa:chatkey:";

    // 工作空间相关
    public static final String ROOT_DIR_NAME = ".apboa";
    public static final String WORKSPACE_DIR_NAME = "workspaces";
    public static final String WORKSPACE_PATH = ROOT_DIR_NAME + "/" + WORKSPACE_DIR_NAME;
    public static final String SKILLS_DIR_NAME = "skills";
    public static final String SKILLS_DIR = ROOT_DIR_NAME + "/" + SKILLS_DIR_NAME;

    // 工作空间钩子错误键
    public static final String WORKSPACE_HOOK_ERROR_KEY = "workspace_hook_error";

    /**
     * 技能文件允许创建的文件扩展名（白名单）
     * params 表 key: allowed_skill_extensions
     */
    public static final String PARAM_KEY_ALLOWED_SKILL_EXTENSIONS = "allowed_skill_extensions";

    /**
     * 技能文件默认允许的扩展名
     */
    public static final List<String> DEFAULT_ALLOWED_SKILL_EXTENSIONS = List.of(
            "md", "py", "sh", "js", "ts", "json", "yaml", "yml", "xml", "txt",
            "java", "cs", "go", "rs", "rb", "php", "sql", "html", "css", "scss", "less", "cfg", "conf", "toml"
    );
}
