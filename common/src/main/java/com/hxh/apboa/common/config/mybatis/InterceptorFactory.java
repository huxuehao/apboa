package com.hxh.apboa.common.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

/**
 * 描述：MyBatisPlus 拦截器工厂
 *
 * @author huxuehao
 **/
public class InterceptorFactory {
    /**
     * 创建分页拦截器
     **/
    public static InnerInterceptor createPaginationInnerInterceptor() {
        return createPaginationInnerInterceptor(DbType.MYSQL);
    }

    /**
     * 创建分页拦截器
     **/
    public static InnerInterceptor createPaginationInnerInterceptor(DbType dbType) {
        return new PaginationInnerInterceptor(dbType);
    }

}
