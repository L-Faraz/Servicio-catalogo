package com.catedra.catalogo.service;

import com.catedra.catalogo.domain.Category;
import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import com.catedra.catalogo.domain.CatalogVersion;
import com.catedra.catalogo.repository.CategoryRepository;
import com.catedra.catalogo.repository.ProfessionalRepository;
import com.catedra.catalogo.repository.WeeklyScheduleRepository;
import com.catedra.catalogo.repository.CatalogVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProfessionalRepository professionalRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final CatalogVersionRepository catalogVersionRepository;

    public CatalogServiceImpl(
            CategoryRepository categoryRepository,
            ProfessionalRepository professionalRepository,
            WeeklyScheduleRepository weeklyScheduleRepository,
            CatalogVersionRepository catalogVersionRepository) {
        this.categoryRepository = categoryRepository;
        this.professionalRepository = professionalRepository;
        this.weeklyScheduleRepository = weeklyScheduleRepository;
        this.catalogVersionRepository = catalogVersionRepository;
    }

    @Override
    public List<Category> getActiveCategories() {
        return categoryRepository.findByEnabledTrue();
    }

    @Override
    public List<Professional> getActiveProfessionals() {
        return professionalRepository.findByEnabledTrue();
    }

    @Override
    public List<Professional> getProfessionalsByCategory(Long categoryId) {
        return professionalRepository.findByCategoryIdAndEnabledTrue(categoryId);
    }

    @Override
    public List<WeeklySchedule> getSchedulesByProfessional(Long professionalId) {
        return weeklyScheduleRepository.findByProfessionalIdAndEnabledTrue(professionalId);
    }

    @Override
    public Optional<CatalogVersion> getCurrentVersion() {
        return catalogVersionRepository.findById(1L);
    }
}