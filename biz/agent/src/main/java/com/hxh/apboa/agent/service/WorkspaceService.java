package com.hxh.apboa.agent.service;

import com.hxh.apboa.common.vo.WorkspaceFileNode;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.List;

/**
 * 描述：工作空间文件管理 Service
 *
 * @author huxuehao
 **/
public interface WorkspaceService {

    /**
     * 上传单个文件到工作空间
     *
     * @param sessionId 会话ID
     * @param file      上传的文件
     * @return 文件在工作空间中的相对路径
     */
    String uploadFile(String sessionId, MultipartFile file);

    /**
     * 上传多个文件到工作空间
     *
     * @param sessionId 会话ID
     * @param files     上传的文件数组
     * @return 各文件在工作空间中的相对路径列表
     */
    List<String> uploadFiles(String sessionId, MultipartFile[] files);

    /**
     * 上传压缩包并自动解压到工作空间
     *
     * @param sessionId 会话ID
     * @param file      压缩包文件
     * @return 解压后的文件相对路径列表
     */
    List<String> uploadAndExtractArchive(String sessionId, MultipartFile file);

    /**
     * 获取工作空间文件树
     *
     * @param sessionId 会话ID
     * @return 文件树根节点列表
     */
    List<WorkspaceFileNode> listFiles(String sessionId);

    /**
     * 下载工作空间中的单个文件
     *
     * @param sessionId    会话ID
     * @param fileName     文件全名
     * @param outputStream 输出流
     */
    void downloadFile(String sessionId, String fileName, OutputStream outputStream);

    /**
     * 下载工作空间中的多个文件（打包成ZIP）
     *
     * @param sessionId    会话ID
     * @param filePaths    文件相对路径列表
     * @param outputStream 输出流
     */
    void downloadFiles(String sessionId, List<String> filePaths, OutputStream outputStream);

    /**
     * 下载整个工作空间（打包成ZIP）
     *
     * @param sessionId    会话ID
     * @param outputStream 输出流
     */
    void downloadWorkspace(String sessionId, OutputStream outputStream);

    /**
     * 删除工作空间中的单个文件或目录
     *
     * @param sessionId 会话ID
     * @param filePath  文件在工作空间中的相对路径
     */
    void deleteFile(String sessionId, String filePath);

    /**
     * 清空工作空间下的所有文件（保留工作空间根目录）
     *
     * @param sessionId 会话ID
     */
    void clearWorkspace(String sessionId);
}
