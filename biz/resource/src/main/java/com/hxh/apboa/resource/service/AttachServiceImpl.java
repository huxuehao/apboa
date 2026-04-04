package com.hxh.apboa.resource.service;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.amazonaws.util.IOUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxh.apboa.common.entity.Attach;
import com.hxh.apboa.common.entity.AttachChunk;
import com.hxh.apboa.common.entity.AttachLog;
import com.hxh.apboa.common.enums.ModelType;
import com.hxh.apboa.common.wrapper.FileBase64Wrapper;
import com.hxh.apboa.params.core.ParamsAdapter;
import com.hxh.apboa.resource.enums.AttachOptType;
import com.hxh.apboa.common.util.FuncUtils;
import com.hxh.apboa.common.util.UserUtils;
import com.hxh.apboa.common.util.WebUtils;
import com.hxh.apboa.resource.mapper.AttachChunkMapper;
import com.hxh.apboa.resource.mapper.AttachMapper;
import com.hxh.apboa.resource.storage.core.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 描述：附件表
 *
 * @author huxuehao
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachServiceImpl extends ServiceImpl<AttachMapper, Attach> implements AttachService {
    private final ParamsAdapter paramsAdapter;
    private final StorageProtocolService storageProtocolService;
    private final AttachLogService attachLogService;
    private final AttachChunkMapper attachChunkMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteV2(List<Long> ids) {
        List<Attach> attaches = listByIds(ids);
        for (Attach attach : attaches) {
            removeById(attach.getId());

            /*
             * NOTE:获取文件存储服务，并删除真实存储的文件，请结合实际，自行决定是否删除真实存储的文件
             */
//            FileStorageService storageService = storageProtocolService.getStorageService();
//            storageService.delete(genStoragePath(attach));

            attachHandelLog(attach, AttachOptType.DELETE);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Attach upload(MultipartFile multipartFile, String originalFilename) {
        String extension = FileNameUtil.getSuffix(originalFilename);
        long size = multipartFile.getSize();

        // 文件大小验证
        double bm = bytesToMB(size);
        double singleFileMaxSize = Double.parseDouble(paramsAdapter.getValue("SINGLE_FILE_MAX_SIZE"));
        if (bm > singleFileMaxSize) {
            throw new RuntimeException("单个文件大小不可超过"+singleFileMaxSize+"MB");
        }

        // 获取文件存储服务
        FileStorageService storageService = storageProtocolService.getStorageService();

        // 保存附件信息
        String storePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        Attach attach = saveAttachInfo(originalFilename, extension, size, storageService.getProtocol(), storePath);

        try (InputStream inputStream = multipartFile.getInputStream()) {
            // 保存文件
            storageService.save(inputStream, genStoragePath(attach));
            // 日志记录
            attachHandelLog(attach, AttachOptType.UPLOAD);
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败", e);
        }
        return attach;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Attach uploadChunkAndMerge(MultipartFile multipartFile,
                               String hash,
                               int totalSize,
                               int index,
                               int totalChunks,
                               String key,
                               String fileName) {
        // 成立条件：只有一个文件分片
        if (totalChunks == 1) {
            // 执行单文件上传
            return upload(multipartFile, fileName);
        }


        // 判断分片是否存在
        if(currentChunkExist(hash, index, key) && totalSize > (index+1)) {
            return new Attach();
        }


        // 获取后缀
        String extension = FileNameUtil.getSuffix(fileName);

        // 记录分片上传
        saveFileChunkInfo(hash,totalSize,index,totalChunks,key,fileName);

        // 存储分片
        try (InputStream inputStream = multipartFile.getInputStream()) {
            // 获取文件存储服务
            FileStorageService storageService = storageProtocolService.getStorageService();
            // 保存文件
            storageService.saveChunk(inputStream, genChunkStoragePath(key, index));

            // 判断文件分片是否上传完成
            if (getChunksByFileKey(key).size() != totalChunks) {
                return new Attach();
            }
            // 保存附件信息
            String storePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            Attach attach = saveAttachInfo(fileName, extension, totalSize, storageService.getProtocol(), storePath);

            // 合并分片上传
            storageService.mergeChunk(genStoragePath(attach), getTotalChunkPath(key, totalChunks));

            // 删除分片记录信息
            deleteFileChunkInfo(key);

            // 日志记录
            attachHandelLog(attach, AttachOptType.UPLOAD);

            return attach;
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败", e);
        }
    }

    /**
     * 判读当前分片是否已经上传过
     * @param chunkHash  分片hash
     * @param chunkIndex 分片索引
     * @param fileKey    文件唯一标识
     */
    private boolean currentChunkExist(String chunkHash, int chunkIndex, String fileKey) {
        List<AttachChunk> attachChunks = getChunksByFileKey(fileKey);
        if (attachChunks !=null && !attachChunks.isEmpty()) {
            List<AttachChunk> collect = attachChunks.stream()
                    .filter(item -> chunkHash.equals(item.getChunkHash()) && chunkIndex == item.getChunkIndex()).
                    toList();

            // 条件成立：当前分片已经上传过
            return !collect.isEmpty();
        }

        return false;
    }

    private List<AttachChunk> getChunksByFileKey(String fileKey) {
        QueryWrapper<AttachChunk> chunkQw = new QueryWrapper<>();
        chunkQw.eq("file_key", fileKey);
        return attachChunkMapper.selectList(chunkQw);
    }

    public void saveFileChunkInfo(String hash, int totalSize, int index, int totalChunks, String key, String fileName) {
        AttachChunk chunk = new AttachChunk();
        chunk.setChunkHash(hash);
        chunk.setChunkIndex(index);
        chunk.setChunkTotals(totalChunks);
        chunk.setFileKey(key);
        chunk.setFileTotalSize(totalSize);
        chunk.setFileName(fileName);
        attachChunkMapper.insert(chunk);
    }
    public void deleteFileChunkInfo(String key) {
        UpdateWrapper<AttachChunk> uw = new UpdateWrapper<>();
        uw.eq("file_key", key);
        attachChunkMapper.delete(uw);
    }

    // 保存附件信息
    private Attach saveAttachInfo(String fileName, String extension, long size, String protocol, String storePath) {
        Attach attach = new Attach();
        attach.setOriginalName(fileName);
        attach.setAttachSize(size);
        attach.setExtension(extension);
        attach.setProtocol(protocol);
        attach.setFileId(IdWorker.getId());
        attach.setPath(storePath);
        Long userId = UserUtils.getId();
        attach.setCreateBy(userId);
        attach.setUpdateBy(userId);
        LocalDateTime now = LocalDateTime.now();
        attach.setCreateAt(now);
        attach.setUpdateAt(now);
        this.baseMapper.insert(attach);
        return attach;
    }

    @Override
    public void download(Attach attach, OutputStream outputStream) {
        //获取文件存储服务
        FileStorageService storageService = storageProtocolService.getStorageService();

        if (!storageService.getProtocol().equals(attach.getProtocol())) {
            throw new RuntimeException("当前文件存储协议为" + attach.getProtocol() + "，与当前启用的存储配置不匹配");
        }

        try (InputStream inputStream = storageService.load(genStoragePath(attach))) {
            IoUtil.copy(inputStream, outputStream);
            attachHandelLog(attach, AttachOptType.DOWNLOAD);
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败", e);
        }
    }

    @Override
    public FileBase64Wrapper getFileBase64(Long fileId) {
        Attach attach = getById(fileId);
        FileBase64Wrapper wrapper = new FileBase64Wrapper();

        String extension = attach.getExtension();
        String resultType = switch (extension) {
            case String ext when paramsAdapter.getValue("ALLOW_IMAGE_FILE_TYPE").contains(ext) -> "IMAGE";
            case String ext when paramsAdapter.getValue("ALLOW_AUDIO_FILE_TYPE").contains(ext) -> "AUDIO";
            case String ext when paramsAdapter.getValue("ALLOW_VIDEO_FILE_TYPE").contains(ext) -> "VIDEO";
            default -> null;
        };

        if (resultType != null) {
            wrapper.setModelType(ModelType.valueOf(resultType));

            // 设置正确的mediaType
            String mediaType = switch (resultType) {
                case "IMAGE" -> "image/" + extension;
                case "AUDIO" -> "audio/" + extension;
                case "VIDEO" -> "video/" + extension;
                default -> null;
            };
            wrapper.setMediaType(mediaType);
        } else {
            return null; // 不支持的文件类型
        }

        //获取文件存储服务
        FileStorageService storageService = storageProtocolService.getStorageService();
        if (!storageService.getProtocol().equals(attach.getProtocol())) {
            log.warn("当前文件存储协议为{}，与当前启用的存储配置不匹配", attach.getProtocol());
            return null;
        }

        // 下载
        try (InputStream inputStream = storageService.load(genStoragePath(attach))) {
            byte[] bytes = IOUtils.toByteArray(inputStream);
            wrapper.setBase64(Base64.getEncoder().encodeToString(bytes));
        } catch (IOException e) {
            return null;
        }

        return wrapper;
    }

    @Override
    public void batchDownload(List<Long> ids, OutputStream outputStream) {
        List<Attach> attaches = listByIds(ids);
        List<Long> ids_ = attaches.stream().map(Attach::getId).collect(Collectors.toList());
        if (FuncUtils.isEmpty(ids_)) {
            throw new RuntimeException("所选附件不存在");
        }

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            final FileStorageService storageService = storageProtocolService.getStorageService();

            // 过滤附件的存储协议
            List<String> protocols = attaches.stream().map(Attach::getProtocol).distinct().toList();

            //存储协议校验
            if (protocols.size() != 1) {
                throw new RuntimeException("所选附件存存在多种存储协议");
            }
            if (!storageService.getProtocol().equals(protocols.getFirst())) {
                throw new RuntimeException("所选文件存储协议为" + protocols.getFirst() + "，与当前启用的存储配置不匹配");
            }

            // 下载附件并打成压缩包
            for (Attach attach : attaches) {
                zipOutputStream.putNextEntry(new ZipEntry(attach.getOriginalName()));
                try (InputStream inputStream = storageService.load(genStoragePath(attach))) {
                    IoUtil.copy(inputStream, zipOutputStream);
                }
                attachHandelLog(attach, AttachOptType.DOWNLOAD);
            }
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败!", e);
        }
    }

    /**
     * 附件操作日志记录
     * @param attach 附件实体
     * @param type   操作类型
     */
    public void attachHandelLog(Attach attach, AttachOptType type) {
        AttachLog log = new AttachLog();
        log.setFileId(attach.getFileId());
        log.setOriginalName(attach.getOriginalName());
        log.setExtension(attach.getExtension());
        log.setAttachSize(attach.getAttachSize());
        log.setOptUser(UserUtils.getId());
        log.setOptUserName(UserUtils.getAccount());
        log.setOptTime(LocalDateTime.now());
        log.setOptIp(WebUtils.getIP());
        log.setOptType(String.valueOf(type));
        attachLogService.save(log);
    }

    /**
     * 生成存储路径
     * @param attach 附件实体
     * @return 路径
     */
    private String genStoragePath(Attach attach) {
        return String.format("%s/%s.%s", attach.getPath(), attach.getFileId(), attach.getExtension());
    }

    /**
     * 生成分片存储路径
     * @param key   唯一标识
     * @param index 当前分片索引
     */
    private String genChunkStoragePath(String key, int index) {
        String uniqueKey = key.replace(".", "_").replace("/","");
        return String.format("%s/%s.%s", "chunks", uniqueKey, "part" + index);
    }

    /**
     * 获取分片路径集合
     * @param key   唯一标识
     * @param total 总数
     */
    private LinkedList<String> getTotalChunkPath(String key, int total) {
        LinkedList<String> paths = new LinkedList<>();
        // 从0开始
        for (int i = 0; i < total; i++) {
            paths.add(genChunkStoragePath(key, i));
        }
        return paths;
    }

    public static double bytesToMB(long bytes) {
        return (double) bytes / (1024 * 1024);
    }
}
