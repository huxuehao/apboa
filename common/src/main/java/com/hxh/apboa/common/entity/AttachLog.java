package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.config.SerializableEnable;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 附件操作日志表
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName("attach_log")
public class AttachLog implements SerializableEnable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文件id
     */
    private Long fileId;

    /**
     * 附件原名
     */
    private String originalName;

    /**
     * 附件拓展名
     */
    private String extension;

    /**
     * 附件大小
     */
    private Long attachSize;

    /**
     * 操作人
     */
    private Long optUser;

    /**
     * 操作人名称
     */
    private String optUserName;

    /**
     * 操作时间
     */
    private LocalDateTime optTime;

    /**
     * 操作IP
     */
    private String optIp;

    /**
     * 操作类型
     */
    private String optType;
}
