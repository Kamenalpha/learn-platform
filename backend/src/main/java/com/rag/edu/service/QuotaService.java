package com.rag.edu.service;

import com.rag.edu.common.BizException;
import com.rag.edu.common.LoginUser;
import com.rag.edu.common.UserContext;
import com.rag.edu.config.QuotaProperties;
import com.rag.edu.mapper.QuotaLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 成本配额:LLM 调用统一出口的检查与记账。
 * 用量 = Redis 当月计数(实时) + quota_log 月度记账(持久);调用前 checkQuota,成功后 record。
 * 管理员不限量;Redis 异常时检查放行(可用性优先),记账失败仅告警。
 */
@Slf4j
@Service
public class QuotaService {

    public static final String CHAT = "chat";
    public static final String GENERATE = "generate";
    public static final String GRADE = "grade";
    public static final String PROJECT = "project";
    public static final String GRAPH = "graph";

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final StringRedisTemplate redis;
    private final QuotaLogMapper quotaLogMapper;
    private final QuotaProperties props;

    public QuotaService(StringRedisTemplate redis, QuotaLogMapper quotaLogMapper, QuotaProperties props) {
        this.redis = redis;
        this.quotaLogMapper = quotaLogMapper;
        this.props = props;
    }

    /** 调用前检查:超限抛 429 业务异常 */
    public void checkQuota(Long userId, String action) {
        if (!props.isEnabled()) {
            return;
        }
        LoginUser user = UserContext.get();
        if (user != null && user.getRole() != null && user.getRole() == 1) {
            return;
        }
        int limit = limitOf(action);
        if (limit <= 0) {
            return;
        }
        try {
            String val = redis.opsForValue().get(key(userId, action));
            if (val != null && Integer.parseInt(val) >= limit) {
                throw new BizException(429, "本月" + nameOf(action) + "额度已用完(上限 "
                        + limit + " 次/月),请联系管理员或下月再试");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("配额检查失败,放行本次调用: {}", e.getMessage());
        }
    }

    /** 调用成功后记账:Redis 当月累加(首次设过期到月底) + quota_log 原子累加 */
    public void record(Long userId, String action) {
        if (!props.isEnabled()) {
            return;
        }
        try {
            String key = key(userId, action);
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofSeconds(secondsToMonthEnd()));
            }
        } catch (Exception e) {
            log.warn("配额 Redis 记账失败: {}", e.getMessage());
        }
        try {
            quotaLogMapper.upsertMonth(userId, action, YearMonth.now().format(MONTH));
        } catch (Exception e) {
            log.warn("配额落库失败: {}", e.getMessage());
        }
    }

    private int limitOf(String action) {
        return switch (action) {
            case CHAT -> props.getChatLimit();
            case GENERATE -> props.getGenerateLimit();
            case GRADE -> props.getGradeLimit();
            case PROJECT -> props.getProjectLimit();
            case GRAPH -> props.getGraphLimit();
            default -> 0;
        };
    }

    static String nameOf(String action) {
        return switch (action) {
            case CHAT -> "智能问答";
            case GENERATE -> "出题";
            case GRADE -> "AI 评分";
            case PROJECT -> "项目辅导";
            case GRAPH -> "知识图谱";
            default -> action;
        };
    }

    static String key(Long userId, String action) {
        return "rag:quota:" + userId + ":" + action + ":" + YearMonth.now().format(MONTH);
    }

    /** 距月底的秒数(INCR 首次过期;下限 60s 防止月末瞬间过期失败) */
    static long secondsToMonthEnd() {
        LocalDateTime end = YearMonth.now().plusMonths(1).atDay(1).atStartOfDay();
        return Math.max(60, Duration.between(LocalDateTime.now(), end).toSeconds());
    }
}
