package com.catedra.catalogo.controller;

import com.catedra.catalogo.domain.Category;
import com.catedra.catalogo.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CatalogService catalogService;

    public CategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<List<Category>> getActiveCategories() {
        return ResponseEntity.ok(catalogService.getActiveCategories());
    }
}