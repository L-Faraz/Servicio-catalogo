package com.catedra.catalogo.repository;

import com.catedra.catalogo.config.CatedraProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class RedisSyncRepository {

    private final StringRedisTemplate redisTemplate;
    private final String readPrefix;

    public RedisSyncRepository(StringRedisTemplate redisTemplate, CatedraProperties properties) {
        this.redisTemplate = redisTemplate;
        this.readPrefix = properties.getEffectiveRedisReadNamespace();
    }

    public String getCurrentVersionKey() {
        return readPrefix + "current-version";
    }

    public String getOldestAvailableVersionKey() {
        return readPrefix + "oldest-available-version";
    }

    public String getMetadataKey() {
        return readPrefix + "metadata";
    }

    public String getProfessionalCategoriesHashKey() {
        return readPrefix + "professional-categories";
    }

    public String getProfessionalsHashKey() {
        return readPrefix + "professionals";
    }

    public String getWeeklySchedulesHashKey() {
        return readPrefix + "weekly-schedules";
    }

    public String getChangeForVersionKey(Long version) {
        return readPrefix + "changes:" + version;
    }

    public Long getCurrentVersion() {
        String version = redisTemplate.opsForValue().get(getCurrentVersionKey());
        return version != null ? Long.parseLong(version) : null;
    }

    public Long getOldestAvailableVersion() {
        String version = redisTemplate.opsForValue().get(getOldestAvailableVersionKey());
        return version != null ? Long.parseLong(version) : null;
    }

    public Map<Object, Object> getMetadata() {
        return redisTemplate.opsForHash().entries(getMetadataKey());
    }

    public String getChangeForVersion(Long version) {
        return redisTemplate.opsForValue().get(getChangeForVersionKey(version));
    }

    public String getEntityFromHash(String hashKey, Long id) {
        Object result = redisTemplate.opsForHash().get(hashKey, String.valueOf(id));
        return result != null ? result.toString() : null;
    }
}
