package com.catedra.catalogo.controller;

import com.catedra.catalogo.dto.CategoryDTO;
import com.catedra.catalogo.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CatalogService catalogService;

    public CategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<CategoryDTO> getCategories(@RequestParam(required = false) Boolean enabled) {
        return catalogService.getCategories(enabled);
    }
}
