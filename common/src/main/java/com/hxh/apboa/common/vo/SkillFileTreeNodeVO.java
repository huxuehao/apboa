package com.hxh.apboa.common.vo;

import com.hxh.apboa.common.config.SerializableEnable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述：技能包文件树节点VO
 *
 * @author huxuehao
 **/
@Data
public class SkillFileTreeNodeVO implements SerializableEnable {

    /** 文件名或目录名 */
    private String name;

    /** 相对路径，如 "scripts/helper.py" */
    private String path;

    /** 是否目录 */
    private boolean directory;

    /** DB id（入库文件才有，纯文件系统文件为 null） */
    private Long fileId;

    /** 文件类型（仅入库文件） */
    private String fileType;

    /** 文件扩展名，目录为空 */
    private String extension;

    /** 文件大小（字节），目录为 0 */
    private long fileSize;

    /** 子节点 */
    private List<SkillFileTreeNodeVO> children = new ArrayList<>();
}
