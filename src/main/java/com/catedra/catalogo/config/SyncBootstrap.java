package com.catedra.catalogo.config;

import com.catedra.catalogo.repository.CatalogVersionRepository;
import com.catedra.catalogo.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SyncBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SyncBootstrap.class);

    private final SyncService syncService;
    private final CatalogVersionRepository versionRepository;
    private final CatedraProperties properties;

    public SyncBootstrap(SyncService syncService, CatalogVersionRepository versionRepository, CatedraProperties properties) {
        this.syncService = syncService;
        this.versionRepository = versionRepository;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            log.warn("Integracion con catedra sin configurar. Se omite la sincronizacion inicial.");
            return;
        }

        long versionCount = versionRepository.count();
        if (versionCount == 0) {
            log.info("Base de datos local vacia. Iniciando sincronizacion completa (Snapshot)...");
            try {
                syncService.syncSnapshot();
                log.info("Sincronizacion inicial completada exitosamente.");
            } catch (Exception e) {
                log.error("Fallo la sincronizacion inicial del catalogo: {}", e.getMessage(), e);
            }
        } else {
            log.info("El catalogo local ya contiene datos. Omitiendo Snapshot inicial.");
        }
    }
}
