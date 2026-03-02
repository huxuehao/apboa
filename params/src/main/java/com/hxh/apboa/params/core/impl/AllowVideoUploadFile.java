package com.hxh.apboa.params.core.impl;

import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.util.FuncUtils;
import com.hxh.apboa.params.core.ParamsAdapter;
import com.hxh.apboa.params.core.ParamsCore;
import org.springframework.stereotype.Component;

/**
 * 描述：允许上传的文件类型
 *
 * @author huxuehao
 **/
@Component
public class AllowVideoUploadFile implements ParamsCore {
    @Override
    public String checkAndFormatValue(String value) {
        if(FuncUtils.isEmpty(value)) {
            throw new RuntimeException("value值不可为空");
        }
        String trim = value.trim();
        trim = trim.replace(" ", ",");
        trim = trim.replace("，", ",");
        trim = trim.replace("、", ",");
        trim = trim.replace(";", ",");
        trim = trim.replace("；", ",");
        return trim;
    }

    @Override
    public String getDefaultValue() {
        return SysConst.ALLOW_VIDEO_FILE_TYPE;
    }

    @Override
    public void register(ParamsAdapter adapter) {
        adapter.register("ALLOW_VIDEO_FILE_TYPE", new AllowVideoUploadFile());
    }
}
