package com.catedra.catalogo.controller;

import com.catedra.catalogo.domain.CatalogVersion;
import com.catedra.catalogo.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog-version")
public class CatalogVersionController {

    private final CatalogService catalogService;

    public CatalogVersionController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<CatalogVersion> getCurrentVersion() {
        return catalogService.getCurrentVersion()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}