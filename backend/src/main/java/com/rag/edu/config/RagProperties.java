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

    /** 允许跨域的前端来源(逗号分隔);生产环境须设为可信域名,勿用 * */
    private String corsAllowedOrigins = "http://localhost:5173,http://localhost:5174";

    /** OCR 服务地址(如 PaddleOCR 服务),为空则关闭 OCR,扫描件将无法解析 */
    private String ocrBaseUrl = "";

    /** OCR 最大识别页数(避免成本失控),默认 30 */
    private int ocrMaxPages = 30;

    /** 重排服务地址(SiliconFlow 兼容 /v1/rerank),为空则重排关闭 */
    private String rerankBaseUrl = "https://api.siliconflow.cn";

    /** 重排模型 */
    private String rerankModel = "BAAI/bge-reranker-v2-m3";

    /** 重排 API Key,为空则重排关闭(可复用 EMBED_API_KEY) */
    private String rerankApiKey = "";
}
