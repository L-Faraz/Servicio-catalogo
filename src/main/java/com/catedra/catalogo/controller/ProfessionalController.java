package com.catedra.catalogo.controller;

import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import com.catedra.catalogo.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professionals")
public class ProfessionalController {

    private final CatalogService catalogService;

    public ProfessionalController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<List<Professional>> getActiveProfessionals(
            @RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return ResponseEntity.ok(catalogService.getProfessionalsByCategory(categoryId));
        }
        return ResponseEntity.ok(catalogService.getActiveProfessionals());
    }

    @GetMapping("/{id}/schedules")
    public ResponseEntity<List<WeeklySchedule>> getSchedulesByProfessional(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getSchedulesByProfessional(id));
    }
}