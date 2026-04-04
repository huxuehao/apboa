package com.hxh.apboa.resource.storage.core;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.hxh.apboa.resource.storage.config.AmazonS3Config;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * 描述：S3对象存储模版
 *
 * @author huxuehao
 **/
@Getter
public class AmazonS3Template {
    private final AmazonS3 amazonS3;

    public AmazonS3Template(AmazonS3Config amazonS3Config) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        AwsClientBuilder.EndpointConfiguration endpointConfiguration =
                new AwsClientBuilder.EndpointConfiguration(
                        amazonS3Config.getEndpoint(),
                        amazonS3Config.getRegion()
                );
        AWSCredentials awsCredentials =
                new BasicAWSCredentials(
                        amazonS3Config.getAccessKey(),
                        amazonS3Config.getSecretKey()
                );
        AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
        this.amazonS3 = AmazonS3Client.builder()
                .withEndpointConfiguration(endpointConfiguration)
                .withClientConfiguration(clientConfiguration)
                .withCredentials(awsCredentialsProvider)
                .disableChunkedEncoding()
                .withPathStyleAccessEnabled(amazonS3Config.getPathStyleAccess())
                .build();
    }


    /**
     * 创建bucket
     * @param bucketName bucket名称
     */
    @SneakyThrows
    public void createBucket(String bucketName) {
        if (!amazonS3.doesBucketExistV2(bucketName)) {
            amazonS3.createBucket((bucketName));
        }
    }

    /**
     * 获取全部bucket
     * <p>
     *
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/ListBuckets">AWS API Documentation</a>
     */
    @SneakyThrows
    public List<Bucket> getAllBuckets() {
        return amazonS3.listBuckets();
    }

    /**
     * @param bucketName bucket名称
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/ListBuckets">AWS API Documentation</a>
     */
    @SneakyThrows
    public Optional<Bucket> getBucket(String bucketName) {
        return amazonS3.listBuckets().stream().filter(b -> b.getName().equals(bucketName)).findFirst();
    }

    /**
     * @param bucketName bucket名称
     * @see <a href=
     * "http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/DeleteBucket">AWS API Documentation</a>
     */
    @SneakyThrows
    public void removeBucket(String bucketName) {
        amazonS3.deleteBucket(bucketName);
    }

    /**
     * 根据文件前置查询文件
     * @param bucketName bucket名称
     * @param prefix 前缀
     * @param recursive 是否递归查询
     * @return S3ObjectSummary 列表
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/ListObjects">AWS API Documentation</a>
     */
    @SneakyThrows
    public List<S3ObjectSummary> getAllObjectsByPrefix(String bucketName, String prefix, boolean recursive) {
        ObjectListing objectListing = amazonS3.listObjects(bucketName, prefix);
        return new ArrayList<>(objectListing.getObjectSummaries());
    }

    /**
     * 获取文件外链
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param expires 过期时间 <=7
     * @return url
     * @see AmazonS3#generatePresignedUrl(String bucketName, String key, Date expiration)
     */
    @SneakyThrows
    public String getObjectURL(String bucketName, String objectName, Integer expires) {
        Date date = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, expires);
        URL url = amazonS3.generatePresignedUrl(bucketName, objectName, calendar.getTime());
        return url.toString();
    }

    /**
     * 获取文件
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @return 二进制流
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/GetObject">AWS API Documentation</a>
     */
    @SneakyThrows
    public InputStream getObject(String bucketName, String objectName) {
        return amazonS3.getObject(bucketName, objectName).getObjectContent();
    }

    /**
     * 上传文件
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param stream 文件流
     */
    public void putObject(String bucketName, String objectName, InputStream stream) throws Exception {
        putObject(bucketName, objectName, stream, stream.available(), "application/octet-stream");
    }

    /**
     * 上传文件
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param stream 文件流
     * @param size 大小
     * @param contextType 类型
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/PutObject">AWS API Documentation</a>
     */
    public PutObjectResult putObject(String bucketName, String objectName, InputStream stream, long size,
                                     String contextType) throws Exception {
        // String fileName = getFileName(objectName);
        byte[] bytes = IOUtils.toByteArray(stream);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(size);
        objectMetadata.setContentType(contextType);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        //创建buket
        createBucket(bucketName);
        // 上传
        return amazonS3.putObject(bucketName, objectName, byteArrayInputStream, objectMetadata);

    }

    /**
     * 获取文件信息
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/GetObject">AWS API Documentation</a>
     */
    public S3Object getObjectInfo(String bucketName, String objectName) throws Exception {
        return amazonS3.getObject(bucketName, objectName);
    }

    /**
     * 删除文件
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/s3-2006-03-01/DeleteObject">AWS APIDocumentation</a>
     */
    public void removeObject(String bucketName, String objectName) throws Exception {
        amazonS3.deleteObject(bucketName, objectName);
    }
}
