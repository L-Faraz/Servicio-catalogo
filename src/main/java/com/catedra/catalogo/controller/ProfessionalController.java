package com.catedra.catalogo.controller;

import com.catedra.catalogo.dto.ProfessionalDTO;
import com.catedra.catalogo.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<List<ProfessionalDTO>> getAllActiveProfessionals() {
        List<ProfessionalDTO> professionals = catalogService.getAllActiveProfessionals();
        return ResponseEntity.ok(professionals);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProfessionalDTO>> getProfessionalsByCategory(@PathVariable Long categoryId) {
        List<ProfessionalDTO> professionals = catalogService.getProfessionalsByCategory(categoryId);
        return ResponseEntity.ok(professionals);
    }

    @PostMapping
    public ResponseEntity<ProfessionalDTO> createProfessional(@RequestBody ProfessionalDTO professionalDTO) {
        ProfessionalDTO createdProfessional = catalogService.createProfessional(professionalDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProfessional);
    }
}