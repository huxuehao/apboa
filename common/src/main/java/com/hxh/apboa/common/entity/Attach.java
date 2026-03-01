package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.config.SerializableEnable;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 附件表
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName("attach")
public class Attach implements SerializableEnable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文件id
     */
    private Long fileId;

    /**
     * 附件地址
     */
    private String link;

    /**
     * 附件域名
     */
    private String domain;

    /**
     * 附件名称
     */
    private String name;

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
     * 存储路径
     */
    private String path;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createAt;

    /**
     * 修改人
     */
    private Long updateBy;

    /**
     * 修改时间
     */
    private LocalDateTime updateAt;

    /**
     * 存储协议
     */
    private String protocol;

    /**
     * 状态
     */
    private Integer status;
}
