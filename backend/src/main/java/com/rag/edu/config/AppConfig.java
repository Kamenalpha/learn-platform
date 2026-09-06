package com.rag.edu.config;

import java.time.Duration;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 通用 Bean 配置。仅引入 spring-security-crypto 做密码加密,
 * 不启用 Spring Security 过滤器链,认证由自定义 JWT 拦截器完成。
 */
@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * RestClient 底层强制 HttpURLConnection(HTTP/1.1)。
     * 默认探测到的 JDK HttpClient 会对明文 HTTP 服务发送 Upgrade: h2c 升级请求,
     * uvicorn 类服务(本地嵌入服务/Chroma)拒绝升级导致请求体丢失(HTTP 422)。
     */
    @Bean
    public ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder() {
        return ClientHttpRequestFactoryBuilder.simple();
    }

    @Bean
    public ClientHttpRequestFactorySettings clientHttpRequestFactorySettings() {
        return ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(10))
                .withReadTimeout(Duration.ofMinutes(5));
    }
}
