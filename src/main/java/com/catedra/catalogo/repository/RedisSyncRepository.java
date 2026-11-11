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
        return redisTemplate.opsForHash().entries("catedra:sync:metadata");
    }

    // Obtiene el JSON con las listas de IDs afectados para una versión específica
    public String getChangeForVersion(Long version) {
        return redisTemplate.opsForValue().get("catedra:sync:changes:" + version);
    }

    // Busca la entidad completa dentro del Hash correspondiente
    public String getEntityFromHash(String hashKey, Long id) {
        Object result = redisTemplate.opsForHash().get(hashKey, String.valueOf(id));
        return result != null ? result.toString() : null;
    }
}