package com.hxh.apboa.common.util;

import com.hxh.apboa.common.UserDetail;
import com.hxh.apboa.common.consts.SysConst;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 描述：用户工具类
 *
 * @author huxuehao
 **/
public class UserUtils {
    public static Long getId() {
        UserDetail userDetail = getUserDetail();
        if (userDetail == null) {
            return 0L;
        }
        return userDetail.getId();
    }

    public static String getAccount() {
        UserDetail userDetail = getUserDetail();
        if (userDetail == null) {
            return null;
        }
        return userDetail.getUsername();
    }

    public static UserDetail getUserDetail() {
        try {
            HttpServletRequest request = WebUtils.getRequest();
            if (request == null) {
                return null;
            }
            return (UserDetail) request.getAttribute(SysConst.USER_DETAIL);
        } catch (IllegalStateException e) {
            // 异步线程或请求生命周期结束后，request 可能已回收，此时按匿名上下文处理。
            return null;
        }
    }
}
