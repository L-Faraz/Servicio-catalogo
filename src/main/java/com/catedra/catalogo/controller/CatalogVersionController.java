package com.catedra.catalogo.controller;

import com.catedra.catalogo.dto.CatalogVersionDTO;
import com.catedra.catalogo.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog-version")
public class CatalogVersionController {

    private final CatalogService catalogService;

    public CatalogVersionController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<CatalogVersionDTO> getCurrentVersion() {
        return catalogService.getCurrentVersion()
                .map(version -> ResponseEntity.ok(
                        new CatalogVersionDTO(version.getCurrentVersion(), version.getLastSyncedAt())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
