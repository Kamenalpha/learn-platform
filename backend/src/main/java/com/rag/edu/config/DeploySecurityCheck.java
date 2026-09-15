package com.rag.edu.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * 部署安全启动校验(代码审查报告 2.1):
 * prod 环境下若 JWT_SECRET 仍为默认/空/弱密钥(长度 &lt; 32),直接拒绝启动,
 * 避免对外暴露可被伪造管理员 token 的默认密钥。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeploySecurityCheck implements ApplicationRunner {

    private static final String DEFAULT_JWT_SECRET =
            "rag-edu-graduation-project-jwt-secret-please-change-in-prod";

    private final RagProperties ragProperties;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        String secret = ragProperties.getJwtSecret();
        if (secret == null || secret.isBlank()
                || DEFAULT_JWT_SECRET.equals(secret) || secret.length() < 32) {
            throw new IllegalStateException(
                    "【安全拦截】prod 环境下 JWT_SECRET 仍为默认/空/弱密钥(需 >=32 字符),"
                            + "已拒绝启动。请通过环境变量 JWT_SECRET 设置强随机密钥,"
                            + "例如: openssl rand -base64 32");
        }
        log.info("prod 环境 JWT_SECRET 校验通过:已使用非默认强密钥");
    }
}
