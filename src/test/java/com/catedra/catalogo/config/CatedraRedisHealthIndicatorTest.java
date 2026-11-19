package com.catedra.catalogo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class CatedraRedisHealthIndicatorTest {

    @Test
    void reportsUpWhenRedisIsNotConfigured() {
        CatedraProperties properties = mock(CatedraProperties.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        CatedraRedisHealthIndicator indicator = new CatedraRedisHealthIndicator(properties, connectionFactory);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("status", "not-configured");
    }

    @Test
    void reportsUpWhenRedisRespondsToPing() {
        CatedraProperties properties = mock(CatedraProperties.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(properties.getRedisHost()).thenReturn("redis.catedra.local");
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        CatedraRedisHealthIndicator indicator = new CatedraRedisHealthIndicator(properties, connectionFactory);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        verify(connection).close();
    }

    @Test
    void reportsDownWhenRedisPingFails() {
        CatedraProperties properties = mock(CatedraProperties.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        when(properties.getRedisHost()).thenReturn("redis.catedra.local");
        when(connectionFactory.getConnection()).thenThrow(new IllegalStateException("sin conexion"));
        CatedraRedisHealthIndicator indicator = new CatedraRedisHealthIndicator(properties, connectionFactory);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
