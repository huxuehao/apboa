package com.hxh.apboa.resource.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxh.apboa.common.entity.StorageProtocol;
import com.hxh.apboa.common.util.FuncUtils;
import com.hxh.apboa.resource.mapper.StorageProtocolMapper;
import com.hxh.apboa.resource.storage.core.FileStorageService;
import com.hxh.apboa.resource.enums.ProtocolType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 描述：文件存储协议配置
 *
 * @author huxuehao
 **/
@Service
public class StorageProtocolServiceImpl extends ServiceImpl<StorageProtocolMapper, StorageProtocol> implements StorageProtocolService {
    @Override
    public boolean saveV2(StorageProtocol body) {
        if(body.getValid() == 1) {
            QueryWrapper<StorageProtocol> qw = new QueryWrapper<>();
            qw.eq("valid", 1);
            qw.ne("id", body.getId());
            List<StorageProtocol> validList = list(qw);
            if(!FuncUtils.isEmpty(validList)) {
                body.setValid(0);
            }
        }
        return save(body);
    }

    @Override
    public boolean updateV2(StorageProtocol body) {
        if(body.getValid() == 1) {
            QueryWrapper<StorageProtocol> qw = new QueryWrapper<>();
            qw.eq("valid", 1);
            qw.ne("id", body.getId());
            List<StorageProtocol> validList = list(qw);
            if(!FuncUtils.isEmpty(validList)) {
                throw new RuntimeException("已存在有效的协议配置");
            }
        }

        UpdateWrapper<StorageProtocol> uw = new UpdateWrapper<>();
        uw.eq("id", body.getId());
        uw.set("name", body.getName());
        uw.set("protocol", body.getProtocol());
        uw.set("valid", body.getValid());
        uw.set("remark", body.getRemark());

        StorageProtocol oldData = getById(body);
        if (!oldData.getProtocol().equals(body.getProtocol())) {
            uw.set("protocol_config", null);
        }

        return update(uw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean validSuccess(Long id) {
        StorageProtocol protocol = getById(id);
        if (protocol.getValid() == 1) {
            return true;
        }
        UpdateWrapper<StorageProtocol> uw = new UpdateWrapper<>();
        uw.set("valid", 0);
        update(uw);

        uw.clear();
        uw.set("valid", 1);
        uw.eq("id", id);
        return update(uw);
    }

    @Override
    public FileStorageService getStorageService() {
        StorageProtocol storageProtocol = getCurrentValidProtocol();
        return createStorageService(storageProtocol);
    }

    /**
     * 获取当前有效的协议
     */
    private StorageProtocol getCurrentValidProtocol() {
        LambdaQueryWrapper<StorageProtocol> qw = Wrappers
                .<StorageProtocol>lambdaQuery()
                .eq(StorageProtocol::getValid, 1);
        List<StorageProtocol> list = list(qw);
        if (FuncUtils.isEmpty(list) || list.size() > 1) {
            throw new RuntimeException("存储配置不存咋唯一一个有效的配置");
        }
        return list.getFirst();
    }

    /**
     * 创建存储服务
     * @param storageProtocol 存储协议实体
     */
    private FileStorageService createStorageService(StorageProtocol storageProtocol) {
        String protocolConfig = storageProtocol.getProtocolConfig();
        if (FuncUtils.isEmpty(protocolConfig)) {
            throw new RuntimeException("请完善协议配置");
        }

        return FileStorageService.create(ProtocolType.valueOf(storageProtocol.getProtocol()), protocolConfig);
    }
}
