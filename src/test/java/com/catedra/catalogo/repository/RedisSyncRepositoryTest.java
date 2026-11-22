package com.catedra.catalogo.repository;

import com.catedra.catalogo.config.CatedraProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisSyncRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisSyncRepository repository;

    @BeforeEach
    void setUp() {
        CatedraProperties properties = new CatedraProperties();
        properties.setRedisReadNamespace("catedra:otro-grupo:*");
        repository = new RedisSyncRepository(redisTemplate, properties);
    }

    @Test
    void usesConfiguredReadNamespaceForAllKeys() {
        assertThat(repository.getCurrentVersionKey()).isEqualTo("catedra:otro-grupo:current-version");
        assertThat(repository.getChangeForVersionKey(7L)).isEqualTo("catedra:otro-grupo:changes:7");
        assertThat(repository.getProfessionalCategoriesHashKey())
                .isEqualTo("catedra:otro-grupo:professional-categories");
    }

    @Test
    void readsCurrentVersionFromNamespacedKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("catedra:otro-grupo:current-version")).thenReturn("419");

        assertThat(repository.getCurrentVersion()).isEqualTo(419L);
        verify(valueOperations).get("catedra:otro-grupo:current-version");
    }
}
