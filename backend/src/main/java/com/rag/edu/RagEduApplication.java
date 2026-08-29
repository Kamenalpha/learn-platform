package com.rag.edu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 基于 RAG 的数媒专业知识库问答系统 - 后端启动类
 */
@SpringBootApplication
@MapperScan("com.rag.edu.mapper")
@ConfigurationPropertiesScan
public class RagEduApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagEduApplication.class, args);
    }
}
