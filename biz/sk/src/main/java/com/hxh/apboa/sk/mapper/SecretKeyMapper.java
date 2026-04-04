package com.hxh.apboa.sk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hxh.apboa.common.entity.SecretKey;
import org.apache.ibatis.annotations.Mapper;

/**
 * 描述：访问秘钥数据访问层
 *
 * @author huxuehao
 **/
@Mapper
public interface SecretKeyMapper extends BaseMapper<SecretKey> {
}
