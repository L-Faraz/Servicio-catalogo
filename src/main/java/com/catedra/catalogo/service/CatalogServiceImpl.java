package com.catedra.catalogo.service;

import com.catedra.catalogo.domain.*;
import com.catedra.catalogo.dto.*;
import com.catedra.catalogo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProfessionalRepository professionalRepository;
    private final CatalogVersionRepository catalogVersionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToCategoryDTO)
                .toList();
    }

    @Override
    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = Category.builder()
                .name(categoryDTO.getName())
                .description(categoryDTO.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        incrementCatalogVersion("Nueva categoría agregada: " + saved.getName());
        return mapToCategoryDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionalDTO> getAllActiveProfessionals() {
        return professionalRepository.findByActiveTrue().stream()
                .map(this::mapToProfessionalDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionalDTO> getProfessionalsByCategory(Long categoryId) {
        return professionalRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToProfessionalDTO)
                .toList();
    }

    @Override
    @Transactional
    public ProfessionalDTO createProfessional(ProfessionalDTO professionalDTO) {
        Category category = categoryRepository.findById(professionalDTO.getCategory().getId())
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

        Professional professional = Professional.builder()
                .firstName(professionalDTO.getFirstName())
                .lastName(professionalDTO.getLastName())
                .email(professionalDTO.getEmail())
                .category(category)
                .active(true)
                .build();

        if (professionalDTO.getSchedules() != null) {
            List<WeeklySchedule> schedules = professionalDTO.getSchedules().stream()
                    .map(dto -> WeeklySchedule.builder()
                            .dayOfWeek(dto.getDayOfWeek())
                            .startTime(dto.getStartTime())
                            .endTime(dto.getEndTime())
                            .professional(professional)
                            .build())
                    .toList();
            professional.setSchedules(schedules);
        }

        Professional saved = professionalRepository.save(professional);
        incrementCatalogVersion("Nuevo profesional agregado: " + saved.getLastName());
        return mapToProfessionalDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogVersionDTO getCurrentCatalogVersion() {
        return catalogVersionRepository.findTopByOrderByVersionNumberDesc()
                .map(this::mapToVersionDTO)
                .orElseGet(() -> CatalogVersionDTO.builder()
                        .versionNumber(1L)
                        .updatedAt(LocalDateTime.now())
                        .description("Versión inicial")
                        .build());
    }

    @Override
    @Transactional
    public CatalogVersionDTO incrementCatalogVersion(String description) {
        Long nextVersion = catalogVersionRepository.findTopByOrderByVersionNumberDesc()
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1L);

        CatalogVersion newVersion = CatalogVersion.builder()
                .versionNumber(nextVersion)
                .updatedAt(LocalDateTime.now())
                .description(description)
                .build();

        CatalogVersion saved = catalogVersionRepository.save(newVersion);
        return mapToVersionDTO(saved);
    }

    // --- Métodos Helper de Mapeo ---

    private CategoryDTO mapToCategoryDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    private ProfessionalDTO mapToProfessionalDTO(Professional professional) {
        List<WeeklyScheduleDTO> schedules = professional.getSchedules() == null ? List.of() :
                professional.getSchedules().stream()
                        .map(s -> WeeklyScheduleDTO.builder()
                                .id(s.getId())
                                .dayOfWeek(s.getDayOfWeek())
                                .startTime(s.getStartTime())
                                .endTime(s.getEndTime())
                                .build())
                        .toList();

        return ProfessionalDTO.builder()
                .id(professional.getId())
                .firstName(professional.getFirstName())
                .lastName(professional.getLastName())
                .email(professional.getEmail())
                .category(mapToCategoryDTO(professional.getCategory()))
                .schedules(schedules)
                .active(professional.isActive())
                .build();
    }

    private CatalogVersionDTO mapToVersionDTO(CatalogVersion version) {
        return CatalogVersionDTO.builder()
                .id(version.getId())
                .versionNumber(version.getVersionNumber())
                .updatedAt(version.getUpdatedAt())
                .description(version.getDescription())
                .build();
    }
}