package com.catedra.catalogo.controller;

import com.catedra.catalogo.dto.CatalogVersionDTO;
import com.catedra.catalogo.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/version")
@RequiredArgsConstructor
public class CatalogVersionController {

    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<CatalogVersionDTO> getCurrentCatalogVersion() {
        CatalogVersionDTO version = catalogService.getCurrentCatalogVersion();
        return ResponseEntity.ok(version);
    }
}