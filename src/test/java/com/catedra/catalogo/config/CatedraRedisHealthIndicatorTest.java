package com.catedra.catalogo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;

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
    void reportsUpWhenRedisAllowsReadingVersionKey() {
        CatedraProperties properties = mock(CatedraProperties.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        RedisKeyCommands keyCommands = mock(RedisKeyCommands.class);
        when(properties.getRedisHost()).thenReturn("redis.catedra.local");
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.keyCommands()).thenReturn(keyCommands);
        when(keyCommands.exists(any(byte[].class))).thenReturn(true);
        CatedraRedisHealthIndicator indicator = new CatedraRedisHealthIndicator(properties, connectionFactory);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("status", "connected");
        verify(connection).close();
    }

    @Test
    void reportsDownWhenRedisReadIsDenied() {
        CatedraProperties properties = mock(CatedraProperties.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        RedisKeyCommands keyCommands = mock(RedisKeyCommands.class);
        when(properties.getRedisHost()).thenReturn("redis.catedra.local");
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.keyCommands()).thenReturn(keyCommands);
        when(keyCommands.exists(any(byte[].class))).thenThrow(new IllegalStateException("sin permisos"));
        CatedraRedisHealthIndicator indicator = new CatedraRedisHealthIndicator(properties, connectionFactory);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        verify(connection).close();
    }
}
