package com.hxh.apboa.common.consts;

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
}
