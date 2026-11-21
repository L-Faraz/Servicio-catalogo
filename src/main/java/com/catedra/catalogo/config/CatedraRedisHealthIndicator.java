package com.catedra.catalogo.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Component("redisHealthIndicator")
public class CatedraRedisHealthIndicator implements HealthIndicator {

    private final CatedraProperties properties;
    private final RedisConnectionFactory connectionFactory;

    public CatedraRedisHealthIndicator(CatedraProperties properties, RedisConnectionFactory connectionFactory) {
        this.properties = properties;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        if (!StringUtils.hasText(properties.getRedisHost())) {
            return Health.up().withDetail("status", "not-configured").build();
        }

        RedisConnection connection = null;
        try {
            connection = connectionFactory.getConnection();
            Boolean versionKeyPresent = connection.keyCommands()
                    .exists("catedra:sync:current-version".getBytes(StandardCharsets.UTF_8));
            String status = Boolean.TRUE.equals(versionKeyPresent) ? "connected" : "connected-no-version-key";
            return Health.up().withDetail("status", status).build();
        } catch (Exception exception) {
            return Health.down(exception).build();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}
