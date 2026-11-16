package com.weixingyu.campusrepairticketsystem.cache;

import cn.hutool.json.JSONUtil;
import com.weixingyu.campusrepairticketsystem.pojo.entity.RedisData;
import com.weixingyu.campusrepairticketsystem.utils.RedisConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class RedisCacheClient {
    private static final Duration CACHE_MISS_WAIT = Duration.ofMillis(500);
    private static final long CACHE_MISS_RETRY_MS = 25;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>("""
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    end
                    return 0
                    """, Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object value, Long time, TimeUnit timeUnit) {
        String jsonStr = JSONUtil.toJsonStr(value);
        stringRedisTemplate.opsForValue().set(key, jsonStr, time, timeUnit);
    }


    public void set(String key, Object value) {
        String jsonStr = JSONUtil.toJsonStr(value);
        stringRedisTemplate.opsForValue().set(key, jsonStr);
    }

    public void set(String key, Object value, Duration time) {
        String jsonStr = JSONUtil.toJsonStr(value);
        stringRedisTemplate.opsForValue().set(key, jsonStr, time);
    }

    public <T> T get(String key, Class<T> c) {
        String jsonStr = stringRedisTemplate.opsForValue().get(key);
        if (jsonStr == null || jsonStr.isBlank()) {
            return null;
        }
        return JSONUtil.toBean(jsonStr, c);
    }

    public <R, ID> R get(String keyPrefix, ID id, Class<R> type,
                         Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        return get(keyPrefix, id, type, dbFallback, time, unit, false);
    }

    public <R, ID> R getWithCacheMissLock(String keyPrefix, ID id, Class<R> type,
                                         Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        return get(keyPrefix, id, type, dbFallback, time, unit, true);
    }

    private <R, ID> R get(String keyPrefix, ID id, Class<R> type,
                          Function<ID, R> dbFallback, Long time, TimeUnit unit,
                          boolean lockOnCacheMiss) {
        String key = keyPrefix + id;
        String lockKey = "lock:" + key;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null && !lockOnCacheMiss) {
            return loadAndCache(key, id, dbFallback, time, unit);
        }
        if (json == null) {
            long deadline = System.nanoTime() + CACHE_MISS_WAIT.toNanos();
            while (json == null) {
                if (System.nanoTime() - deadline >= 0) {
                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE, "缓存正在重建，请稍后重试");
                }

                String token = tryLock(lockKey);
                if (token != null) {
                    try {
                        // 另一个请求可能刚回填并释放了锁。
                        json = stringRedisTemplate.opsForValue().get(key);
                        if (json == null) {
                            return loadAndCache(key, id, dbFallback, time, unit);
                        }
                    } finally {
                        unlock(lockKey, token);
                    }
                    break;
                }

                try {
                    Thread.sleep(CACHE_MISS_RETRY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE, "等待缓存重建被中断", e);
                }
                json = stringRedisTemplate.opsForValue().get(key);
            }
        }
        if (json.isEmpty()) {
            return null;
        }
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        String dataJson = JSONUtil.toJsonStr(redisData.getData());
        R value = type.isArray()
                ? type.cast(JSONUtil.parseArray(dataJson).toArray(type.getComponentType()))
                : JSONUtil.toBean(dataJson, type);
        if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
            return value;
        }

        String token = tryLock(lockKey);
        if (token != null) {
            Thread.startVirtualThread(() -> {
                try {
                    R freshValue = dbFallback.apply(id);
                    if (freshValue != null) {
                        setWithLogicalExpire(key, freshValue, time, unit);
                    }
                } finally {
                    unlock(lockKey, token);
                }
            });
        }
        return value;
    }

    private <R, ID> R loadAndCache(String key, ID id, Function<ID, R> dbFallback,
                                  Long time, TimeUnit unit) {
        R value = dbFallback.apply(id);
        if (value == null) {
            stringRedisTemplate.opsForValue().set(
                    key, "", RedisConstants.NULL_OBJECT_TTL, TimeUnit.MINUTES);
        } else {
            setWithLogicalExpire(key, value, time, unit);
        }
        return value;
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        Duration ttl = Duration.of(time, unit.toChronoUnit());
        RedisData data = new RedisData(LocalDateTime.now().plus(ttl), value);
        set(key, data, ttl.plusMinutes(10));
    }

    public void delete(String key){
        stringRedisTemplate.delete(key);
    }

    public void deleteAfterCommit(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                delete(key);
            }
        });
    }

    private String tryLock(String lockKey) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, token, Duration.ofSeconds(30));
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    private void unlock(String lockKey, String token) {
        stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), token);
    }

}
