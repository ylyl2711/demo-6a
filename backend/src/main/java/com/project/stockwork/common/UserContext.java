package com.project.stockwork.common;

public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(401, "未登录或登录已失效");
        }
        return user;
    }

    public static String name() {
        LoginUser user = HOLDER.get();
        return user == null ? "" : user.getRealName();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
