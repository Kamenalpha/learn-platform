package com.rag.edu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 基于 RAG 的通用多学科智能学习平台 - 后端启动类
 */
@SpringBootApplication
@MapperScan("com.rag.edu.mapper")
@ConfigurationPropertiesScan
@EnableScheduling // 知识资讯每日 9:00 定时抓取
public class RagEduApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagEduApplication.class, args);
    }
}
