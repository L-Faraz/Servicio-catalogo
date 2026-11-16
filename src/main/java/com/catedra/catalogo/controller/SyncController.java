package com.catedra.catalogo.controller;

import com.catedra.catalogo.dto.CatalogSnapshotDTO;
import com.catedra.catalogo.dto.SyncStatusDTO;
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

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/snapshot")
    public ResponseEntity<CatalogSnapshotDTO> syncSnapshot() {
        return ResponseEntity.ok(syncService.syncSnapshot());
    }

    @PostMapping("/incremental")
    public ResponseEntity<Void> syncIncremental() {
        syncService.processIncrementalSync();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public SyncStatusDTO getStatus() {
        return syncService.getStatus();
    }
}
