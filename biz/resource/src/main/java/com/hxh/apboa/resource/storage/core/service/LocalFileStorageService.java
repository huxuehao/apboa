package com.hxh.apboa.resource.storage.core.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.hxh.apboa.resource.storage.config.LocalStorageConfig;
import com.hxh.apboa.resource.storage.core.FileStorageService;
import com.hxh.apboa.resource.enums.ProtocolType;

import java.io.*;
import java.util.LinkedList;

/**
 * 描述：本地存储服务实现
 *
 * @author huxuehao
 **/
public class LocalFileStorageService implements FileStorageService {
    private final LocalStorageConfig localStorageConfig;

    public LocalFileStorageService(LocalStorageConfig localStorageConfig) {
        this.localStorageConfig =localStorageConfig;
    }

    @Override
    public String getProtocol() {
        return ProtocolType.LOCAL.name();
    }

    @Override
    public void save(InputStream inputStream, String path) {
        File file = new File(this.getFullFilePath(path));
        copyInputStreamToFile(inputStream, file);
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

        try(FileOutputStream fileOutputStream = createFileOutputStream(filePath, true)){
            // 遍历分片，并追加合并
            for (String chunkPath : chunkPaths) {
                // 加载分片bytes
                byte[] chunkBytes = loadBytes(chunkPath);
                // 合并
                fileOutputStream.write(chunkBytes);
                // 删除分片
                delete(chunkPath);
            }
        }catch (IOException e) {
            throw new RuntimeException("分片文件合并失败",e);
        }
    }

    // 将输入流复制到文件
    public void copyInputStreamToFile(InputStream inputStream, File file) {
        OutputStream outputStream = null;
        try {
            // 获取文件输出流
            outputStream = FileUtil.getOutputStream(file);
            // 使用 IoUtil.copy 进行流拷贝 [citation:2][citation:5]
            IoUtil.copy(inputStream, outputStream, IoUtil.DEFAULT_BUFFER_SIZE);
        } finally {
            // 关闭流（IoUtil.close 可安全关闭，会判空）[citation:2]
            IoUtil.close(outputStream);
            IoUtil.close(inputStream);
        }
    }

    /**
     * 创建文件输出流，文件不存在则创建文件
     * @param path   文件路径
     * @param append 是否可追加
     */
    public FileOutputStream createFileOutputStream(String path, boolean append) throws IOException {
        File file = new File(getFullFilePath(path));

        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException(file + " 是个文件夹，无法进行写入");
            }

            if (!file.canWrite()) {
                throw new IOException(file + " 不允许写入");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
                throw new IOException("文件件 " + parent + " 无法被创建");
            }
        }

        return new FileOutputStream(file, append);
    }

    @Override
    public void delete(String path) {
        File file = new File(this.getFullFilePath(path));
        if (file.exists()) {
            if (!file.delete()) {
                throw new RuntimeException("文件删除失败");
            }
        }
    }

    @Override
    public InputStream load(String path) {
        File file = new File(this.getFullFilePath(path));

        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("文件不存在", e);
        }
    }

    public byte[] loadBytes(String path) {
        try (InputStream inputStream = load(path);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[1024];
            int nRead;
            // 循环从 InputStream 中读取数据
            while ((nRead = inputStream.read(data, 0, data.length))!= -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            // 将 ByteArrayOutputStream 中的数据转换为 byte 数组
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("文件加载转换失败", e);
        }
    }


    public String getLocalDir() {
        return localStorageConfig.getLocalDir();
    }

    private String getFullFilePath(String relativePath) {
        // 将路径分隔符换成当前系统的分隔符
        String fullPath  = this.getLocalDir() + File.separator + relativePath.replace("/", File.separator);

        // 兼容win、linux
        return fullPath.replace("\\", "\\\\");
    }
}
