package com.catedra.catalogo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(CatedraProperties properties) {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();

        // Usamos los valores dinámicos que guardamos previamente de la API de integración
        redisConfig.setHostName(properties.getRedisHost());
        redisConfig.setPort(properties.getRedisPort());
        redisConfig.setUsername(properties.getRedisUsername());
        redisConfig.setPassword(properties.getRedisPassword());

        return new LettuceConnectionFactory(redisConfig);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // StringRedisTemplate es ideal porque el contrato dicta que las claves y valores en Redis son Strings/JSON
        return new StringRedisTemplate(redisConnectionFactory);
    }
}