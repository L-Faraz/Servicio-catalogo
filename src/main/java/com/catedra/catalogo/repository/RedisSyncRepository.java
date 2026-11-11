package com.catedra.catalogo.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public List<String> getEventsBetween(Long fromVersion, Long toVersion) {
        List<Object> keys = new ArrayList<>();
        for (long v = fromVersion; v <= toVersion; v++) {
            keys.add(String.valueOf(v));
        }

        // Recupera los JSONs de los eventos faltantes desde el hash "catedra:sync:events"
        List<Object> events = redisTemplate.opsForHash().multiGet("catedra:sync:events", keys);

        if (events == null) {
            return List.of();
        }

        return events.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
    }
}