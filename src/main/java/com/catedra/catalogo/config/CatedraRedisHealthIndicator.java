package com.catedra.catalogo.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
            String response = connection.ping();
            if ("PONG".equalsIgnoreCase(response)) {
                return Health.up().build();
            }
            return Health.down().withDetail("response", response).build();
        } catch (Exception exception) {
            return Health.down(exception).build();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}
