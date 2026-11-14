package com.catedra.catalogo.service;

import com.catedra.catalogo.domain.CatalogVersion;
import com.catedra.catalogo.domain.Category;
import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import com.catedra.catalogo.dto.CategoryDTO;
import com.catedra.catalogo.dto.ProfessionalDTO;
import com.catedra.catalogo.dto.ProfessionalDetailDTO;
import com.catedra.catalogo.dto.WeeklyScheduleDTO;
import com.catedra.catalogo.exception.ResourceNotFoundException;
import com.catedra.catalogo.repository.CatalogVersionRepository;
import com.catedra.catalogo.repository.CategoryRepository;
import com.catedra.catalogo.repository.ProfessionalRepository;
import com.catedra.catalogo.repository.ProfessionalSpecifications;
import com.catedra.catalogo.repository.WeeklyScheduleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProfessionalRepository professionalRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final CatalogVersionRepository catalogVersionRepository;
    private final CatalogMapper mapper;

    public CatalogServiceImpl(CategoryRepository categoryRepository,
                              ProfessionalRepository professionalRepository,
                              WeeklyScheduleRepository weeklyScheduleRepository,
                              CatalogVersionRepository catalogVersionRepository,
                              CatalogMapper mapper) {
        this.categoryRepository = categoryRepository;
        this.professionalRepository = professionalRepository;
        this.weeklyScheduleRepository = weeklyScheduleRepository;
        this.catalogVersionRepository = catalogVersionRepository;
        this.mapper = mapper;
    }

    @Override
    public List<CategoryDTO> getCategories(Boolean enabled) {
        List<Category> categories = enabled == null
                ? categoryRepository.findAll(Sort.by("name"))
                : categoryRepository.findByEnabled(enabled);
        return categories.stream().map(mapper::toCategoryDto).toList();
    }

    @Override
    public Page<ProfessionalDTO> searchProfessionals(Long categoryId,
                                                     String name,
                                                     Boolean enabled,
                                                     DayOfWeek dayOfWeek,
                                                     LocalTime time,
                                                     Pageable pageable) {
        Specification<Professional> specification = (root, query, cb) -> cb.conjunction();
        specification = specification.and(ProfessionalSpecifications.hasCategory(categoryId));
        specification = specification.and(ProfessionalSpecifications.nameContains(name));
        specification = specification.and(ProfessionalSpecifications.hasEnabled(enabled));
        specification = specification.and(ProfessionalSpecifications.hasAvailability(dayOfWeek, time));

        return professionalRepository.findAll(specification, pageable)
                .map(mapper::toProfessionalDto);
    }

    @Override
    public ProfessionalDetailDTO getProfessional(Long id) {
        Professional professional = professionalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el profesional " + id));
        List<WeeklySchedule> schedules = weeklyScheduleRepository.findByProfessionalId(id);
        return mapper.toProfessionalDetailDto(professional, schedules);
    }

    @Override
    public List<WeeklyScheduleDTO> getSchedulesByProfessional(Long professionalId) {
        if (!professionalRepository.existsById(professionalId)) {
            throw new ResourceNotFoundException("No existe el profesional " + professionalId);
        }
        return weeklyScheduleRepository.findByProfessionalIdAndEnabledTrue(professionalId)
                .stream()
                .map(mapper::toWeeklyScheduleDto)
                .toList();
    }

    @Override
    public Optional<CatalogVersion> getCurrentVersion() {
        return catalogVersionRepository.findById(1L);
    }
}
