package com.catedra.catalogo.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Configuration
public class RedisConfig {

    private static final Duration REDIS_TIMEOUT = Duration.ofSeconds(2);

    @Bean
    public RedisConnectionFactory redisConnectionFactory(CatedraProperties properties) {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();

        String host = properties.getRedisHost();
        if (!StringUtils.hasText(host)) {
            throw new IllegalStateException("catedra.integration.redis-host es obligatorio");
        }
        int port = properties.getRedisPort() > 0 ? properties.getRedisPort() : 6379;

        redisConfig.setHostName(host);
        redisConfig.setPort(port);

        if (StringUtils.hasText(properties.getRedisUsername())) {
            redisConfig.setUsername(properties.getRedisUsername());
        }
        if (StringUtils.hasText(properties.getRedisPassword())) {
            redisConfig.setPassword(properties.getRedisPassword());
        }

        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(REDIS_TIMEOUT)
                .build();
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .timeoutOptions(TimeoutOptions.enabled(REDIS_TIMEOUT))
                .build();
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(REDIS_TIMEOUT)
                .clientOptions(clientOptions)
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
