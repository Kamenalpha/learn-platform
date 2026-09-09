package com.rag.edu.service;

import com.rag.edu.config.RagProperties;
import com.rag.edu.dto.KbDtos.KbConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 检索参数配置(相似度阈值、召回数量、分块大小等)。
 * 存储在 Redis Hash 中,未配置时回落到 application.yml 默认值。
 */
@Service
@RequiredArgsConstructor
public class KbConfigService {

    public static final String KEY = "rag:kb:config";

    private final StringRedisTemplate redis;
    private final RagProperties props;

    public KbConfig get() {
        var entries = redis.opsForHash().entries(KEY);
        KbConfig def = new KbConfig(props.getDefaultTopK(), props.getDefaultThreshold(),
                props.getChunkSize(), props.getChunkOverlap(), "", true);
        String topK = (String) entries.get("topK");
        String threshold = (String) entries.get("similarityThreshold");
        String chunkSize = (String) entries.get("chunkSize");
        String overlap = (String) entries.get("chunkOverlap");
        String suffix = (String) entries.get("promptSuffix");
        String rerank = (String) entries.get("rerankEnabled");
        return new KbConfig(
                topK == null ? def.topK() : Integer.parseInt(topK),
                threshold == null ? def.similarityThreshold() : Double.parseDouble(threshold),
                chunkSize == null ? def.chunkSize() : Integer.parseInt(chunkSize),
                overlap == null ? def.chunkOverlap() : Integer.parseInt(overlap),
                suffix == null ? def.promptSuffix() : suffix,
                rerank == null ? def.rerankEnabled() : Boolean.parseBoolean(rerank));
    }

    public void update(KbConfig config) {
        if (config.topK() != null) {
            redis.opsForHash().put(KEY, "topK", String.valueOf(config.topK()));
        }
        if (config.similarityThreshold() != null) {
            redis.opsForHash().put(KEY, "similarityThreshold", String.valueOf(config.similarityThreshold()));
        }
        if (config.chunkSize() != null) {
            redis.opsForHash().put(KEY, "chunkSize", String.valueOf(config.chunkSize()));
        }
        if (config.chunkOverlap() != null) {
            redis.opsForHash().put(KEY, "chunkOverlap", String.valueOf(config.chunkOverlap()));
        }
        if (config.promptSuffix() != null) {
            redis.opsForHash().put(KEY, "promptSuffix", config.promptSuffix());
        }
        if (config.rerankEnabled() != null) {
            redis.opsForHash().put(KEY, "rerankEnabled", String.valueOf(config.rerankEnabled()));
        }
    }
}
