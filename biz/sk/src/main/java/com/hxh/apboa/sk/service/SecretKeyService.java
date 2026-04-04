package com.hxh.apboa.sk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxh.apboa.common.entity.SecretKey;
import com.hxh.apboa.common.vo.SecretKeyVo;

import java.util.List;

/**
 * 描述：访问秘钥业务接口
 *
 * @author huxuehao
 **/
public interface SecretKeyService extends IService<SecretKey> {

    /**
     * 创建秘钥
     *
     * @param vo 秘钥参数
     * @return 创建后的秘钥VO（含value）
     */
    SecretKeyVo create(SecretKeyVo vo);

    /**
     * 更新秘钥（仅允许更新名称）
     *
     * @param vo 秘钥参数（需含id和name）
     * @return 是否成功
     */
    boolean updateName(SecretKeyVo vo);

    /**
     * 删除秘钥
     *
     * @param ids ID列表
     * @return 是否成功
     */
    boolean delete(List<Long> ids);

    /**
     * 查询全部秘钥列表（value已脱敏）
     *
     * @return 秘钥列表
     */
    List<SecretKeyVo> listAll();
}
