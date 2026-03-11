package com.hxh.apboa.job.core.annotation;

import java.lang.annotation.*;

/**
 * 描述：移除定时
 *
 * @author huxuehao
 **/
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QuartzRemove {
}
