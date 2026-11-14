package com.catedra.catalogo.controller;

import com.catedra.catalogo.dto.ProfessionalDTO;
import com.catedra.catalogo.dto.ProfessionalDetailDTO;
import com.catedra.catalogo.dto.WeeklyScheduleDTO;
import com.catedra.catalogo.service.CatalogService;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/professionals")
public class ProfessionalController {

    private final CatalogService catalogService;

    public ProfessionalController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public Page<ProfessionalDTO> searchProfessionals(
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "true") Boolean enabled,
            @RequestParam(required = false) DayOfWeek dayOfWeek,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm[:ss]") LocalTime time,
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return catalogService.searchProfessionals(categoryId, name, enabled, dayOfWeek, time, pageable);
    }

    @GetMapping("/{id}")
    public ProfessionalDetailDTO getProfessional(@PathVariable @Positive Long id) {
        return catalogService.getProfessional(id);
    }

    @GetMapping("/{id}/schedules")
    public List<WeeklyScheduleDTO> getSchedules(@PathVariable @Positive Long id) {
        return catalogService.getSchedulesByProfessional(id);
    }
}
