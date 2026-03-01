package com.hxh.apboa.resource.storage.config;

import lombok.Getter;
import lombok.Setter;

/**
 * 描述：对象存储（S3）
 *
 * @author huxuehao
 **/
@Setter
@Getter
public class AmazonS3Config {
    private String endpoint;
    private String customDomain;
    private Boolean pathStyleAccess = true;
    private String appId;
    private String region;
    private String accessKey;
    private String secretKey;
    private String bucketName = "etm";
}
