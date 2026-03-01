package com.hxh.apboa.resource.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.entity.Attach;
import com.hxh.apboa.common.entity.AttachLog;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.mp.support.PageParams;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.util.FuncUtils;
import com.hxh.apboa.resource.service.AttachLogService;
import com.hxh.apboa.resource.service.AttachService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 描述：附件表
 *
 * @author huxuehao
 **/
@RestController
@RequestMapping("/attach")
@RequiredArgsConstructor
public class AttachController {
    private final AttachService attachService;
    private final AttachLogService attachLogService;

    @GetMapping(value = "/page", name = "分页")
    public R<IPage<Attach>> page(Attach attach, PageParams pageParams) {
        QueryWrapper<Attach> qw = MP.getQueryWrapper(attach);
        qw.last("ORDER BY create_time DESC");
        return R.data(attachService.page(MP.getPage(pageParams), qw));
    }

    @GetMapping(value = "/log/page", name = "日志:分页")
    public R<IPage<AttachLog>> LogPage(AttachLog attachLog, PageParams pageParams) {
        QueryWrapper<AttachLog> qw = MP.getQueryWrapper(attachLog);
        qw.last("ORDER BY opt_time DESC");
        return R.data(attachLogService.page(MP.getPage(pageParams), qw));
    }

    @RoleNeed({Role.ADMIN, Role.EDIT})
    @PostMapping(value = "/delete", name = "删除")
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        return R.data(attachService.batchDeleteV2(ids));
    }

    @GetMapping(value = "/selectOne", name = "根据ID唯一获取")
    public R<Attach> selectOne(@RequestParam Long id) {
        return R.data(attachService.getById(id));
    }

    @PostMapping(value = "/list", name = "列表")
    public R<List<Attach>> selectList(@RequestBody List<Long> ids) {
        return R.data(attachService.listByIds(ids));
    }

    /**
     * 上传
     */
    @PostMapping(value = "/upload", name = "上传")
    public R<String> upload(@RequestParam("file") MultipartFile file) {
        final Attach attach = attachService.upload(file, file.getOriginalFilename());
        return R.data(String.valueOf(attach.getId()));
    }

    /**
     * 分片串行上传
     */
    @PostMapping(value = "/chunk-upload", name = "分片串行上传")
    public R<String> uploadChunk(@RequestParam("file") MultipartFile file,
                              @RequestParam("hash") String hash,
                              @RequestParam("totalSize") int totalSize,
                              @RequestParam("index") int index,
                              @RequestParam("totalChunks") int totalChunks,
                              @RequestParam("uniqueKey") String uniqueKey,
                              @RequestParam("fileName") String fileName) {
        Attach attach = attachService.uploadChunkAndMerge(file, hash, totalSize, index, totalChunks, uniqueKey, fileName);
        return R.data(String.valueOf(attach.getId()));
    }

    /**
     * 下载
     */
    @GetMapping(value = "download/{id}", name = "下载")
    public void download(@PathVariable("id") Long id, HttpServletResponse response) {
        Attach attach = attachService.getById(id);
        if (FuncUtils.isEmpty(attach)) {
            throw new RuntimeException("附件不存在");
        }
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        try (OutputStream outputStream = response.getOutputStream()) {
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment;filename=" + URLEncoder.encode(attach.getOriginalName(), "UTF-8"));
            this.attachService.download(attach, outputStream);
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败", e);
        }
    }

    /**
     * 批量下载
     */
    @PostMapping(value = "batchDownload", name = "批量下载")
    public void batchDownload(@RequestBody List<Long> ids, HttpServletResponse response) {
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        try (OutputStream outputStream = response.getOutputStream()) {
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment;filename=" + URLEncoder.encode("批量下载.zip", StandardCharsets.UTF_8));
            this.attachService.batchDownload(ids, outputStream);
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败！", e);
        }
    }
}
