package com.weixingyu.campusrepairticketsystem.cache;

import cn.hutool.json.JSONUtil;
import com.weixingyu.campusrepairticketsystem.pojo.entity.RedisData;
import com.weixingyu.campusrepairticketsystem.utils.RedisConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class RedisCacheClient {
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
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            R value = dbFallback.apply(id);
            if (value == null) {
                stringRedisTemplate.opsForValue().set(
                        key, "", RedisConstants.NULL_OBJECT_TTL, TimeUnit.MINUTES);
                return null;
            }
            setWithLogicalExpire(key, value, time, unit);
            return value;
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

        String lockKey = "lock:" + key;
        if (Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey, "1", Duration.ofSeconds(30)))) {
            Thread.startVirtualThread(() -> {
                try {
                    R freshValue = dbFallback.apply(id);
                    if (freshValue != null) {
                        setWithLogicalExpire(key, freshValue, time, unit);
                    }
                } finally {
                    stringRedisTemplate.delete(lockKey);
                }
            });
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

}
