package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.config.SerializableEnable;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文件存储协议配置
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName("storage_protocol")
public class StorageProtocol implements SerializableEnable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 存储协议
     */
    private String protocol;

    /**
     * 协议配置
     */
    private String protocolConfig;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createAt;

    /**
     * 修改人
     */
    private String updateBy;

    /**
     * 修改时间
     */
    private LocalDateTime updateAt;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否有效
     */
    private Integer valid;
}
