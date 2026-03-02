package com.hxh.apboa.common.wrapper;

import com.hxh.apboa.common.enums.ModelType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述：FileBase64 包装类
 *
 * @author huxuehao
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileBase64Wrapper {
    String base64;
    String mediaType;
    ModelType modelType;
}
