package com.catedra.catalogo.config;

import com.catedra.catalogo.domain.SyncStatus;
import com.catedra.catalogo.repository.SyncStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("testcontainers")
class PostgresFlywayIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SyncStatusRepository syncStatusRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
    }

    @Test
    void flywaySchemaMatchesEntitiesAndPersistsSyncState() {
        assertThat(syncStatusRepository.findById(1L))
                .get()
                .extracting(SyncStatus::getState)
                .isEqualTo("IDLE");

        SyncStatus status = syncStatusRepository.findById(1L).orElseThrow();
        status.setState("ERROR");
        status.setLastError("fallo de prueba");
        syncStatusRepository.saveAndFlush(status);

        assertThat(syncStatusRepository.findById(1L).orElseThrow().getState()).isEqualTo("ERROR");

        Integer emailColumns = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_name = 'professionals' and column_name = 'email'",
                Integer.class);
        assertThat(emailColumns).isZero();
    }
}
