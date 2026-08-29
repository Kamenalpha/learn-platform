package com.rag.edu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 业务参数(对应 application.yml 中 rag.* 配置)
 */
@Data
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    /** 课件文件存储目录 */
    private String uploadDir = "./uploads";

    /** 递归字符分块:每块目标字符数 */
    private int chunkSize = 500;

    /** 相邻块重叠字符数 */
    private int chunkOverlap = 50;

    /** 向量嵌入批量大小 */
    private int embedBatchSize = 16;

    /** 默认召回数量 */
    private int defaultTopK = 5;

    /** 默认相似度阈值 */
    private double defaultThreshold = 0.5;

    /** JWT 签名密钥 */
    private String jwtSecret;

    /** JWT 过期时间(小时) */
    private long jwtExpireHours = 72;
}
