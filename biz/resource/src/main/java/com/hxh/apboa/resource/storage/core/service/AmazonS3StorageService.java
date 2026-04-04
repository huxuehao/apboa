package com.hxh.apboa.resource.storage.core.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.hxh.apboa.resource.storage.config.AmazonS3Config;
import com.hxh.apboa.resource.storage.core.AmazonS3Template;
import com.hxh.apboa.resource.storage.core.FileStorageService;
import com.hxh.apboa.resource.enums.ProtocolType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * 描述：S3存储服务实现（支持所有兼容aws-s3协议的对象存储）
 *
 * @author huxuehao
 **/
public class AmazonS3StorageService implements FileStorageService {
    private final AmazonS3Config amazonS3Config;

    private final AmazonS3Template amazonS3Template;

    public AmazonS3StorageService(AmazonS3Config amazonS3Config) {
        this.amazonS3Config = amazonS3Config;
        this.amazonS3Template = new AmazonS3Template(amazonS3Config);
    }

    @Override
    public String getProtocol() {
        return ProtocolType.S3.name();
    }

    @Override
    public void save(InputStream inputStream, String path) {
        final String bucket = this.amazonS3Config.getBucketName();

        try (InputStream is = inputStream) {
            amazonS3Template.putObject(bucket, path, is);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public void saveChunk(InputStream inputStream, String chunkPath) {
        save(inputStream, chunkPath);
    }

    @Override
    public void mergeChunk(String filePath, LinkedList<String> chunkPaths) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 逐个读取分片并合并
        for (String chunkPath : chunkPaths) {
            try (InputStream chunkInputStream = load(chunkPath)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = chunkInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                throw new RuntimeException("读取文件分片失败: " + chunkPath, e);
            }
        }

        // 将合并后的文件上传到S3
        byte[] mergedFileBytes = outputStream.toByteArray();
        ByteArrayInputStream mergedInputStream = new ByteArrayInputStream(mergedFileBytes);
        save(mergedInputStream, filePath);

        // 删除分片文件
        for (String chunkPath : chunkPaths) {
            delete(chunkPath);
        }
    }

    /**
     * 分片上传V2：第一步.初始化分片上传任务
     *
     * @param path       文件在 S3 中的存储路径
     * @return UploadId，用于后续的分片上传和合并
     */
    public String initiateChunkUpload(String path) {
        final String bucketName = this.amazonS3Config.getBucketName();
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, path);
        AmazonS3 amazonS3 = amazonS3Template.getAmazonS3();
        InitiateMultipartUploadResult initResponse = amazonS3.initiateMultipartUpload(initRequest);
        return initResponse.getUploadId();
    }

    /**
     * 分片上传V2：第二步.上传分片
     *
     * @param path        文件在 S3 中的存储路径
     * @param uploadId    分片上传任务的 UploadId
     * @param partNumber  分片编号（从 1 开始）
     * @param inputStream 分片的输入流
     * @return 分片的 ETag
     */
    public String uploadChunk(String path, String uploadId, int partNumber, InputStream inputStream) throws IOException {
        final String bucketName = this.amazonS3Config.getBucketName();
        UploadPartRequest uploadRequest = new UploadPartRequest()
                .withBucketName(bucketName)
                .withKey(path)
                .withUploadId(uploadId)
                .withPartNumber(partNumber)
                .withInputStream(inputStream)
                .withPartSize(inputStream.available());

        AmazonS3 amazonS3 = amazonS3Template.getAmazonS3();
        UploadPartResult uploadResult = amazonS3.uploadPart(uploadRequest);
        return uploadResult.getETag();
    }

    /**
     * 分片上传V2：第三步.完成分片上传
     *
     * @param path        文件在 S3 中的存储路径
     * @param uploadId    分片上传任务的 UploadId
     * @param partETags   分片的 ETag 列表
     */
    public void completeChunkUpload(String path, String uploadId, List<PartETag> partETags) {
        final String bucketName = this.amazonS3Config.getBucketName();
        CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(bucketName, path, uploadId, partETags);
        AmazonS3 amazonS3 = amazonS3Template.getAmazonS3();
        amazonS3.completeMultipartUpload(completeRequest);
    }


    @Override
    public void delete(String path) {
        final String bucket = this.amazonS3Config.getBucketName();

        try {
            amazonS3Template.removeObject(bucket, path);
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败", e);
        }
    }

    @Override
    public InputStream load(String path) {
        final String bucket = this.amazonS3Config.getBucketName();
        return amazonS3Template.getObject(bucket, path);
    }
}
