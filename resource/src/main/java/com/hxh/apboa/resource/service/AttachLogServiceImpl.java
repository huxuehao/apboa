package com.hxh.apboa.resource.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxh.apboa.common.entity.AttachLog;
import com.hxh.apboa.resource.mapper.AttachLogMapper;
import org.springframework.stereotype.Service;

/**
 * 描述：附件操作日志
 *
 * @author huxuehao
 **/
@Service
public class AttachLogServiceImpl extends ServiceImpl<AttachLogMapper, AttachLog> implements AttachLogService {
}
