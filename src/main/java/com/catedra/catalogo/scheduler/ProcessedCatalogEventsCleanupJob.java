package com.catedra.catalogo.scheduler;

import com.catedra.catalogo.config.CatedraProperties;
import com.catedra.catalogo.repository.ProcessedCatalogEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "catedra.integration.processed-events-cleanup-enabled", havingValue = "true", matchIfMissing = true)
public class ProcessedCatalogEventsCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ProcessedCatalogEventsCleanupJob.class);

    private final ProcessedCatalogEventRepository repository;
    private final CatedraProperties properties;

    public ProcessedCatalogEventsCleanupJob(ProcessedCatalogEventRepository repository, CatedraProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${catedra.integration.processed-events-cleanup-interval-ms:3600000}",
            initialDelayString = "${catedra.integration.processed-events-cleanup-initial-delay-ms:60000}"
    )
    public void cleanup() {
        long retentionDays = properties.getProcessedEventsRetentionDays();
        if (retentionDays <= 0) {
            return;
        }

        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        long deleted = repository.deleteByProcessedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Eventos procesados eliminados: {}", deleted);
        }
    }
}
