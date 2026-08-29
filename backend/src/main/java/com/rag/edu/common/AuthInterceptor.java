package com.rag.edu.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器:
 * - /api/** 全部拦截(登录/注册已排除)
 * - /api/admin/** 额外要求管理员角色
 * - 支持 Authorization: Bearer xxx 或 ?token=xxx(用于浏览器直接打开文件预览链接)
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }
        if (token == null || token.isBlank()) {
            throw new BizException(401, "未登录或登录已过期");
        }
        LoginUser user;
        try {
            user = jwtUtil.parse(token);
        } catch (Exception e) {
            throw new BizException(401, "未登录或登录已过期");
        }
        UserContext.set(user);
        if (request.getRequestURI().startsWith("/api/admin") && (user.getRole() == null || user.getRole() != 1)) {
            throw new BizException(403, "无权限,仅管理员可访问");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
