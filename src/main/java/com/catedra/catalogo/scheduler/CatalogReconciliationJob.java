package com.catedra.catalogo.scheduler;

import com.catedra.catalogo.config.CatedraProperties;
import com.catedra.catalogo.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "catedra.integration.reconciliation-enabled", havingValue = "true", matchIfMissing = true)
public class CatalogReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(CatalogReconciliationJob.class);

    private final SyncService syncService;
    private final CatedraProperties properties;

    public CatalogReconciliationJob(SyncService syncService, CatedraProperties properties) {
        this.syncService = syncService;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${catedra.integration.reconciliation-interval-ms:60000}",
            initialDelayString = "${catedra.integration.reconciliation-initial-delay-ms:30000}"
    )
    public void reconcile() {
        if (!properties.isConfigured()) {
            return;
        }

        try {
            syncService.processIncrementalSync();
        } catch (RuntimeException e) {
            log.warn("Reconciliacion de catalogo fallida: {}", e.getMessage());
        }
    }
}
