package com.hxh.apboa.core.tool.builtins;

import com.hxh.apboa.common.r.R;
import com.hxh.apboa.core.tool.IAgentTool;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

/**
 * 描述：获取当前时间
 *
 * @author huxuehao
 **/
@Component
public class GetCurrentTimeTool implements IAgentTool {

    @Tool(name = "get_current_datetime", description = "获取当前的日期时间")
    public Object getCurrentDateTime(
            @ToolParam(
                    name = "format",
                    description = "日期时间格式，默认值 yyyy-MM-dd HH:mm:ss",
                    required = false)
            String format) {

        if (format == null || format.isEmpty()) {
            format = "yyyy-MM-dd HH:mm:ss";
        }

        return R.data(new SimpleDateFormat(format).format(System.currentTimeMillis()));
    }
}
