package com.hxh.apboa.resource.storage.config;

import lombok.Getter;
import lombok.Setter;

/**
 * 描述：Ftp存储
 *
 * @author huxuehao
 **/
@Setter
@Getter
public class FtpStorageConfig {
    private String host;
    private Integer port = 21;
    private String userName;
    private String password;
    private String encoding = "UTF-8";
}
