package com.catedra.catalogo.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class RedisSyncRepository {

    private final StringRedisTemplate redisTemplate;

    public RedisSyncRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Long getCurrentVersion() {
        String version = redisTemplate.opsForValue().get("catedra:sync:current-version");
        return version != null ? Long.parseLong(version) : null;
    }

    public Long getOldestAvailableVersion() {
        String version = redisTemplate.opsForValue().get("catedra:sync:oldest-available-version");
        return version != null ? Long.parseLong(version) : null;
    }

    public Map<Object, Object> getMetadata() {
        // La metadata es un Hash de strings según el contrato
        return redisTemplate.opsForHash().entries("catedra:sync:metadata");
    }
}