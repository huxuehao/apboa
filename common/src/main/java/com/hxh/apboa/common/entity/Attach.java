package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.mp.annotation.QueryDefine;
import com.hxh.apboa.common.mp.support.QueryCondition;
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
    @QueryDefine(condition = QueryCondition.EQ)
    private Long fileId;

    /**
     * 附件地址
     */
    @QueryDefine(condition = QueryCondition.LIKE)
    private String link;

    /**
     * 附件域名
     */
    @QueryDefine(condition = QueryCondition.LIKE)
    private String domain;

    /**
     * 附件名称
     */
    @QueryDefine(condition = QueryCondition.LIKE)
    private String name;

    /**
     * 附件原名
     */
    @QueryDefine(condition = QueryCondition.LIKE)
    private String originalName;

    /**
     * 附件拓展名
     */
    @QueryDefine(condition = QueryCondition.LIKE)
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
