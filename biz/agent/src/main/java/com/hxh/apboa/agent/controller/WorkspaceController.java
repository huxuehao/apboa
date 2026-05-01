package com.hxh.apboa.agent.controller;

import com.hxh.apboa.agent.service.WorkspaceService;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.vo.WorkspaceFileNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 描述：智能体工作空间文件管理 Controller
 *
 * @author huxuehao
 **/
@RestController
@RequestMapping("/agent/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    /**
     * 上传单个文件到工作空间
     */
    @PostMapping("/upload")
    public R<String> upload(@RequestParam("sessionId") String sessionId,
                            @RequestParam("file") MultipartFile file) {
        String path = workspaceService.uploadFile(sessionId, file);
        return R.data(path, "上传成功");
    }

    /**
     * 上传多个文件到工作空间
     */
    @PostMapping("/upload/batch")
    public R<List<String>> uploadBatch(@RequestParam("sessionId") String sessionId,
                                       @RequestParam("files") MultipartFile[] files) {
        List<String> paths = workspaceService.uploadFiles(sessionId, files);
        return R.data(paths, "批量上传成功");
    }

    /**
     * 上传压缩包并自动解压到工作空间
     */
    @PostMapping("/upload/archive")
    public R<List<String>> uploadArchive(@RequestParam("sessionId") String sessionId,
                                          @RequestParam("file") MultipartFile file) {
        List<String> paths = workspaceService.uploadAndExtractArchive(sessionId, file);
        return R.data(paths, "压缩包解压成功");
    }

    /**
     * 获取工作空间文件树
     */
    @GetMapping("/files")
    public R<List<WorkspaceFileNode>> listFiles(@RequestParam("sessionId") String sessionId) {
        List<WorkspaceFileNode> nodes = workspaceService.listFiles(sessionId);
        return R.data(nodes);
    }

    /**
     * 下载工作空间中的单个文件
     */
    @GetMapping("/download")
    public void download(@RequestParam("sessionId") String sessionId,
                         @RequestParam("fileName") String fileName,
                         HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        setDownloadHeader(response, fileName);

        try (OutputStream outputStream = response.getOutputStream()) {
            workspaceService.downloadFile(sessionId, fileName, outputStream);
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败", e);
        }
    }

    /**
     * 下载工作空间中的多个文件（打包成ZIP）
     */
    @PostMapping("/download/batch")
    public void downloadBatch(@RequestParam("sessionId") String sessionId,
                              @RequestBody List<String> filePaths,
                              HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        setDownloadHeader(response, "batch-download.zip");

        try (OutputStream outputStream = response.getOutputStream()) {
            workspaceService.downloadFiles(sessionId, filePaths, outputStream);
        } catch (Exception e) {
            throw new RuntimeException("批量下载失败", e);
        }
    }

    /**
     * 下载整个工作空间（打包成ZIP）
     */
    @GetMapping("/download/all")
    public void downloadAll(@RequestParam("sessionId") String sessionId,
                            HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        setDownloadHeader(response, "workspace-" + sessionId + ".zip");

        try (OutputStream outputStream = response.getOutputStream()) {
            workspaceService.downloadWorkspace(sessionId, outputStream);
        } catch (Exception e) {
            throw new RuntimeException("工作空间下载失败", e);
        }
    }

    /**
     * 删除工作空间中的单个文件
     */
    @DeleteMapping("/file")
    public R<Void> deleteFile(@RequestParam("sessionId") String sessionId,
                              @RequestParam("filePath") String filePath) {
        workspaceService.deleteFile(sessionId, filePath);
        return R.success("删除成功");
    }

    /**
     * 清空工作空间下的所有文件
     */
    @DeleteMapping("/clear")
    public R<Void> clearWorkspace(@RequestParam("sessionId") String sessionId) {
        workspaceService.clearWorkspace(sessionId);
        return R.success("清空成功");
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 设置文件下载响应头
     */
    private void setDownloadHeader(HttpServletResponse response, String fileName) {
        try {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        } catch (Exception e) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment;filename=download");
        }
    }
}
