package com.hxh.apboa.sk.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxh.apboa.common.config.auth.AuthInterceptor;
import com.hxh.apboa.common.config.auth.SkIdSyncPublisher;
import com.hxh.apboa.common.entity.SecretKey;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.util.TokenUtils;
import com.hxh.apboa.common.vo.SecretKeyVo;
import com.hxh.apboa.sk.mapper.SecretKeyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;

/**
 * 描述：访问秘钥业务实现
 *
 * @author huxuehao
 **/
@Service
@RequiredArgsConstructor
public class SecretKeyServiceImpl extends ServiceImpl<SecretKeyMapper, SecretKey> implements SecretKeyService {

    private final SkIdSyncPublisher skIdSyncPublisher;

    /** 秘钥前缀 */
    private static final String SK_PREFIX = "sk-";

    /** 20年对应的毫秒数 */
    private static final long TEN_YEARS_MILLIS = 20L * 365 * 24 * 60 * 60 * 1000;

    @Override
    public SecretKeyVo create(SecretKeyVo vo) {
        // 构建实体并保存，让框架分配ID
        SecretKey entity = new SecretKey();
        entity.setName(vo.getName());
        entity.setExpireTime(vo.getExpireTime());
        entity.setRemark(vo.getRemark());
        save(entity);

        // 计算过期时间（毫秒）
        long ttlMillis;
        if (vo.getExpireTime() == null) {
            ttlMillis = TEN_YEARS_MILLIS;
        } else {
            ttlMillis = vo.getExpireTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli() - System.currentTimeMillis();
        }

        // 生成token，id为SK的数据库ID，subject为name，并压缩以缩短存储长度
        String token = TokenUtils.createToken(String.valueOf(entity.getId()), vo.getName(), ttlMillis);
        String value = SK_PREFIX + TokenUtils.compressJwt(token);

        // 更新value字段
        entity.setValue(value);

        updateById(entity);

        // 将SK ID添加到本地有效集合中
        AuthInterceptor.addSkId(entity.getId());

        // 发布同步消息通知其他节点
        skIdSyncPublisher.publishAdd(entity.getId());

        return BeanUtils.copy(entity, SecretKeyVo.class);
    }

    @Override
    public boolean updateName(SecretKeyVo vo) {
        return lambdaUpdate()
                .eq(SecretKey::getId, vo.getId())
                .set(SecretKey::getName, vo.getName())
                .update();
    }

    @Override
    public boolean delete(List<Long> ids) {
        boolean success = removeByIds(ids);
        if (success) {
            // 从本地有效集合中移除已删除的SK ID
            AuthInterceptor.removeSkIds(ids);

            // 发布同步消息通知其他节点
            skIdSyncPublisher.publishRemove(ids);
        }
        return success;
    }

    @Override
    public List<SecretKeyVo> listAll() {
        List<SecretKey> list = lambdaQuery().list();
        List<SecretKeyVo> result = BeanUtils.copyList(list, SecretKeyVo.class);
        // 对value进行脱敏处理
        result.forEach(vo -> vo.setValue(maskValue(vo.getValue())));
        return result;
    }

    /**
     * 对秘钥value进行脱敏，保留前缀及首尾少量字符，中间用*替代
     *
     * @param value 原始value
     * @return 脱敏后的value
     */
    private String maskValue(String value) {
        if (value == null || value.length() <= 16) {
            return value;
        }
        // 保留前10个字符 + **** + 后4个字符
        return value.substring(0, 10) + "****" + value.substring(value.length() - 10);
    }
}
