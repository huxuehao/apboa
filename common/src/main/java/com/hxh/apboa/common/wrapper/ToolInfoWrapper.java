package com.hxh.apboa.common.wrapper;

import lombok.*;

import java.util.List;

/**
 * 描述：工具信息包装类
 *
 * @author huxuehao
 **/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolInfoWrapper {
    private String name;
    private String description;
    private List<ParamInfo> params;
    private String classPath;

    public static class ParamInfo {
        @Getter
        @Setter
        private String name;
        @Getter
        @Setter
        private String description;
        @Getter
        @Setter
        private String type;
        @Getter
        @Setter
        private String defaultValue;
        @Getter
        @Setter
        private Boolean required;
    }
}
