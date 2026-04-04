package com.hxh.apboa.resource.storage.core.service;

import com.hxh.apboa.resource.storage.config.FtpStorageConfig;
import com.hxh.apboa.resource.storage.core.FileStorageService;
import com.hxh.apboa.resource.enums.ProtocolType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

/**
 * 描述：Ftp存储服务实现
 *
 * @author huxuehao
 **/
@Slf4j
public class FtpFileStorageService implements FileStorageService {
    private final FtpStorageConfig ftpStorageConfig;

    public FtpFileStorageService(FtpStorageConfig ftpStorageConfig) {
        this.ftpStorageConfig = ftpStorageConfig;
    }

    @Override
    public String getProtocol() {
        return ProtocolType.FTP.name();
    }

    @Override
    public void save(InputStream inputStream, String path) {
        final FTPClient ftpClient = this.createFtpClient();
        this.changePath(ftpClient, path);
        this.upload(ftpClient, inputStream, path);
    }

    @Override
    public void saveChunk(InputStream inputStream, String chunkPath) {
        save(inputStream, chunkPath);
    }

    @Override
    public void mergeChunk(String filePath, LinkedList<String> chunkPaths) {
        if (chunkPaths == null || chunkPaths.isEmpty()) {
            return;
        }
        //连接 FTP 服务器
        FTPClient ftpClient = createFtpClient();
        try {
            // 遍历分片，并追加合并
            for (String chunkPath : chunkPaths) {
                // 切换到分片存储的路径下
                this.changePath(ftpClient, chunkPath);
                // 获取分片数据流
                try(InputStream chunkInputStream = ftpClient.retrieveFileStream(this.getFileName(chunkPath))) {
                    // 切换到正式文件的存储目录
                    this.changePath(ftpClient, filePath);
                    // 将分片文件增量保留正式文件
                    ftpClient.appendFile(this.getFileName(chunkPath), chunkInputStream);
                    // 删除临时分片文件
                    delete(chunkPath);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } finally {
            this.disconnect(ftpClient);
        }
    }

    @Override
    public void delete(String path) {
        final FTPClient ftpClient = this.createFtpClient();
        this.changePath(ftpClient, path);
        try {
            ftpClient.deleteFile(this.getFileName(path));
        } catch (IOException e) {
            throw new RuntimeException("文件删除失败", e);
        }
    }

    @Override
    public InputStream load(String path) {
        final FTPClient ftpClient = this.createFtpClient();
        this.changePath(ftpClient, path);
        try {
            return ftpClient.retrieveFileStream(this.getFileName(path));
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败", e);
        }
    }

    private FTPClient createFtpClient() {
        FTPClient ftpClient = new FTPClient();
        ftpClient.setControlEncoding(this.ftpStorageConfig.getEncoding());

        try {
            ftpClient.connect(this.ftpStorageConfig.getHost(), this.ftpStorageConfig.getPort());

            // 连接后检测返回码来校验连接是否成功
            int reply = ftpClient.getReplyCode();

            if (FTPReply.isPositiveCompletion(reply)) {
                //登陆到ftp服务器
                if (ftpClient.login(this.ftpStorageConfig.getUserName(), this.ftpStorageConfig.getPassword())) {
                    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE); // 文件类型设置
                    ftpClient.enterLocalPassiveMode(); // 进入本地被动模式
                } else {
                    throw new RuntimeException("FTP服务器登录失败");
                }

            } else {
                ftpClient.disconnect();
                throw new RuntimeException("FTP服务器连接失败");
            }

        } catch (IOException e) {
            try {
                ftpClient.disconnect(); //断开连接
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
            }
            throw new RuntimeException("FTP服务器连接失败", e);
        }

        return ftpClient;
    }

    private void changePath(FTPClient ftpClient, String path) {
        path = path.replace("/", File.separator);
        final String directory = this.getDirectory(path);

        if (directory == null || directory.isEmpty() || File.separator.equals(directory)) {
            return;
        }

        // 兼容linux和win
        final String[] directories = directory.split(File.separator.replace("\\", "\\\\"));

        StringBuilder sbDir = new StringBuilder();

        try {
            for (String dir : directories) {
                //sbDir.append(File.separator);
                sbDir.append(dir);

                // 先尝试切换目录
                if (ftpClient.changeWorkingDirectory(sbDir.toString())) {
                    continue;
                }

                // 尝试切换目录失败后再创建目录
                if (!ftpClient.makeDirectory(dir)) {
                    throw new RuntimeException("FTP目录创建失败");
                }

                // 再次尝试切换目录
                ftpClient.changeWorkingDirectory(sbDir.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException("FTP目录切换失败");
        }
    }

    /**
     * 获取文件名
     * @param path 路径
     */
    private String getFileName(String path) {
        if (!path.contains(File.separator)) {
            return path;
        }
        return path.substring(path.lastIndexOf(File.separator) + 1);
    }

    private void upload(FTPClient ftpClient, InputStream inputStream, String path) {
        try {
            final String fileName = this.getFileName(path);
            final boolean storeFile = ftpClient.storeFile(new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1), inputStream);
            if (!storeFile) {
                throw new RuntimeException("FTP文件上传失败");
            }
        } catch (IOException e) {
            throw new RuntimeException("FTP文件上传失败");
        } finally {
            this.disconnect(ftpClient);
            try {
                inputStream.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 获取目录
     *
     * @param path 基于路径获取目录
     */
    private String getDirectory(String path) {
        final int i = path.lastIndexOf(File.separator);
        if (i < 0) {
            return null;
        }
        return path.substring(0, i);
    }

    private void disconnect(FTPClient ftpClient) {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

    }
}
