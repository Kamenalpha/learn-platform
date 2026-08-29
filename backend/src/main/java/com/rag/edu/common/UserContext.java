package com.rag.edu.common;

/**
 * 当前登录用户 ThreadLocal 上下文
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        return HOLDER.get() == null ? null : HOLDER.get().getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 当前用户必须是管理员,否则抛 403 */
    public static LoginUser requireAdmin() {
        LoginUser user = HOLDER.get();
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            throw new BizException(403, "无权限,该操作仅管理员可用");
        }
        return user;
    }
}
