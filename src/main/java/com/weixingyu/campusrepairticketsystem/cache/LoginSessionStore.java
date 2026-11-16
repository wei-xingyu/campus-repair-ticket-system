package com.weixingyu.campusrepairticketsystem.cache;

import com.weixingyu.campusrepairticketsystem.pojo.entity.LoginUser;
import com.weixingyu.campusrepairticketsystem.utils.RedisConstants;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class LoginSessionStore {

    @Resource
    private RedisCacheClient redisCacheClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void save(String token, LoginUser user){
        String key = RedisConstants.USER_TOKENS_PREFIX +user.getId();
        stringRedisTemplate.opsForSet().add(key,token);
        redisCacheClient.set(RedisConstants.LOGIN_USER_KEY+token,user,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
    }

    public void revokeOne(String token,Long userId){
        redisCacheClient.delete(RedisConstants.LOGIN_USER_KEY+token);
        stringRedisTemplate.opsForSet().remove(RedisConstants.USER_TOKENS_PREFIX +userId,token);
    }

    public void revokeAll(Long userId){
        String indexKey = RedisConstants.USER_TOKENS_PREFIX + userId;
        Set<String> tokens = stringRedisTemplate.opsForSet().members(indexKey);
        if (tokens != null && !tokens.isEmpty()) {
            List<String> sessionKeys = tokens.stream()
                    .map(token -> RedisConstants.LOGIN_USER_KEY + token)
                    .toList();
            stringRedisTemplate.delete(sessionKeys);
        }
        stringRedisTemplate.delete(indexKey);
    }
}
