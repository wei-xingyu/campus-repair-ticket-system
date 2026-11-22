package com.weixingyu.campusrepairticketsystem;

import cn.hutool.json.JSONUtil;
import com.weixingyu.campusrepairticketsystem.cache.RedisCacheClient;
import com.weixingyu.campusrepairticketsystem.pojo.entity.RedisData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisCacheClientTest {
    private static final String KEY = "ticket:detail:42";
    private static final String LOCK_KEY = "lock:" + KEY;

    private final RedisCacheClient cache = new RedisCacheClient();
    private ValueOperations<String, String> values;
    private RLock lock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        RedissonClient redisson = mock(RedissonClient.class);
        lock = mock(RLock.class);
        when(redisson.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        ReflectionTestUtils.setField(cache, "redissonClient", redisson);
        when(redis.opsForValue()).thenReturn(values);
        ReflectionTestUtils.setField(cache, "stringRedisTemplate", redis);
    }

    @Test
    void reusesCacheFilledBeforeLockWasAcquired() {
        when(values.get(KEY)).thenReturn(null, cachedDetail());
        when(lock.tryLock()).thenReturn(true);

        assertArrayEquals(new String[]{"detail"}, readCachedDetail());
    }

    @Test
    void waitsForTheOtherRequestToFillTheCache() {
        when(values.get(KEY)).thenReturn(null, cachedDetail());
        when(lock.tryLock()).thenReturn(false);

        assertArrayEquals(new String[]{"detail"}, readCachedDetail());
    }

    private String[] readCachedDetail() {
        return cache.getWithCacheMissLock("ticket:detail:", 42L, String[].class,
                id -> fail("A cache filled by another request should be reused"),
                10L, TimeUnit.MINUTES);
    }

    private String cachedDetail() {
        return JSONUtil.toJsonStr(new RedisData(
                LocalDateTime.now().plusMinutes(10), new String[]{"detail"}));
    }
}
