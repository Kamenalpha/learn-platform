package com.rag.edu.service;

import com.rag.edu.common.BizException;
import com.rag.edu.common.LoginUser;
import com.rag.edu.common.UserContext;
import com.rag.edu.config.QuotaProperties;
import com.rag.edu.mapper.QuotaLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuotaServiceTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    private final QuotaLogMapper quotaLogMapper = mock(QuotaLogMapper.class);
    private final QuotaProperties props = new QuotaProperties();
    private final QuotaService service = new QuotaService(redis, quotaLogMapper, props);

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void overLimitIsRejectedWithFriendlyMessage() {
        props.setChatLimit(2);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(QuotaService.key(1L, QuotaService.CHAT))).thenReturn("2");

        BizException error = assertThrows(BizException.class,
                () -> service.checkQuota(1L, QuotaService.CHAT));

        assertEquals(429, error.getCode());
        assertTrue(error.getMessage().contains("智能问答"));
        assertTrue(error.getMessage().contains("2 次/月"));
    }

    @Test
    void underLimitIsAllowed() {
        props.setChatLimit(200);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("199");

        assertDoesNotThrow(() -> service.checkQuota(1L, QuotaService.CHAT));
    }

    @Test
    void adminBypassesQuota() {
        props.setChatLimit(1);
        UserContext.set(new LoginUser(9L, "admin", 1));
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("999");

        assertDoesNotThrow(() -> service.checkQuota(9L, QuotaService.CHAT));
    }

    @Test
    void zeroLimitMeansUnlimited() {
        props.setGraphLimit(0);
        assertDoesNotThrow(() -> service.checkQuota(1L, QuotaService.GRAPH));
        verify(redis, never()).opsForValue();
    }

    @Test
    void redisFailureFailsOpen() {
        props.setChatLimit(200);
        when(redis.opsForValue()).thenThrow(new RuntimeException("redis down"));

        assertDoesNotThrow(() -> service.checkQuota(1L, QuotaService.CHAT));
    }

    @Test
    void recordIncrementsRedisAndUpsertsDb() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);

        service.record(1L, QuotaService.GENERATE);

        verify(valueOps).increment(QuotaService.key(1L, QuotaService.GENERATE));
        verify(redis).expire(eq(QuotaService.key(1L, QuotaService.GENERATE)), any(Duration.class));
        verify(quotaLogMapper).upsertMonth(eq(1L), eq(QuotaService.GENERATE), anyString());
    }

    @Test
    void quotaCanBeDisabledGlobally() {
        props.setEnabled(false);
        assertDoesNotThrow(() -> service.checkQuota(1L, QuotaService.CHAT));
        verify(redis, never()).opsForValue();
    }

    @Test
    void expireDurationIsAtLeastOneMinuteAndKeysAreMonthlyScoped() {
        assertTrue(QuotaService.secondsToMonthEnd() >= 60);
        String key = QuotaService.key(1L, QuotaService.CHAT);
        assertTrue(key.startsWith("rag:quota:1:chat:"));
        assertTrue(key.substring("rag:quota:1:chat:".length()).matches("\\d{6}"));
    }
}
