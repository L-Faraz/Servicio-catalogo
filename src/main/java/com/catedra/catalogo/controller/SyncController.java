package com.catedra.catalogo.controller;

import com.catedra.catalogo.config.CatedraProperties;
import com.catedra.catalogo.dto.CatalogSnapshotDTO;
import com.catedra.catalogo.dto.SyncStatusDTO;
import com.catedra.catalogo.exception.IntegrationNotConfiguredException;
import com.catedra.catalogo.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/synchronization")
public class SyncController {

    private final SyncService syncService;
    private final CatedraProperties properties;

    public SyncController(SyncService syncService, CatedraProperties properties) {
        this.syncService = syncService;
        this.properties = properties;
    }

    @PostMapping("/snapshot")
    public ResponseEntity<CatalogSnapshotDTO> syncSnapshot() {
        ensureProvisioningReady();
        return ResponseEntity.ok(syncService.syncSnapshot());
    }

    @PostMapping("/incremental")
    public ResponseEntity<Void> syncIncremental() {
        ensureProvisioningReady();
        syncService.processIncrementalSync();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public SyncStatusDTO getStatus() {
        return syncService.getStatus();
    }

    private void ensureProvisioningReady() {
        if (properties.isProvisioningBlocked()) {
            throw new IntegrationNotConfiguredException("El provisioning de la catedra no esta finalizado");
        }
    }
}
