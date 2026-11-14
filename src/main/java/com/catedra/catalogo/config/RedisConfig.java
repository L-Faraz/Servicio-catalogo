package com.catedra.catalogo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(CatedraProperties properties) {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();

        String host = StringUtils.hasText(properties.getRedisHost())
                ? properties.getRedisHost()
                : "localhost";
        int port = properties.getRedisPort() > 0 ? properties.getRedisPort() : 6379;

        redisConfig.setHostName(host);
        redisConfig.setPort(port);

        if (StringUtils.hasText(properties.getRedisUsername())) {
            redisConfig.setUsername(properties.getRedisUsername());
        }
        if (StringUtils.hasText(properties.getRedisPassword())) {
            redisConfig.setPassword(properties.getRedisPassword());
        }

        return new LettuceConnectionFactory(redisConfig);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
