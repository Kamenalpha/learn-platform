package com.rag.edu.config;

import com.rag.edu.common.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置:跨域 + JWT 认证拦截器
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RagProperties ragProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login", "/api/auth/register",
                        // 游客(未登录)可访问的公开只读接口:知识资讯 + 公开课程内容
                        "/api/news/list", "/api/news/detail/**", "/api/public/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String origins = ragProperties.getCorsAllowedOrigins();
        String[] patterns = (origins != null && !origins.isBlank())
                ? origins.split(",")
                : new String[]{"http://localhost:5173", "http://localhost:5174"};
        registry.addMapping("/api/**")
                .allowedOriginPatterns(patterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
