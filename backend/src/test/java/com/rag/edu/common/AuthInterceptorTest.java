package com.rag.edu.common;

import jakarta.servlet.http.HttpServletRequest;import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 越权回归:普通用户访问 /api/admin/** 必须 403(参照 2026-09-07 数据隔离越权回归的做法)
 */
class AuthInterceptorTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final AuthInterceptor interceptor = new AuthInterceptor(jwtUtil);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    private void loginAs(int role) {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(request.getRequestURI()).thenReturn("/api/admin/stats/ai-usage");
        when(jwtUtil.parse("token")).thenReturn(new LoginUser(2L, "user", role));
    }

    @Test
    void ordinaryUserCallingAdminApiIsRejectedWith403() {
        loginAs(0);

        BizException error = assertThrows(BizException.class,
                () -> interceptor.preHandle(request, response, new Object()));

        assertEquals(403, error.getCode());
    }

    @Test
    void adminCallingAdminApiPasses() {
        loginAs(1);

        assertDoesNotThrow(() -> interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void ordinaryUserCallingNonAdminApiPasses() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(request.getRequestURI()).thenReturn("/api/exam/mistakes");
        when(jwtUtil.parse("token")).thenReturn(new LoginUser(2L, "user", 0));

        assertDoesNotThrow(() -> interceptor.preHandle(request, response, new Object()));
    }
}
