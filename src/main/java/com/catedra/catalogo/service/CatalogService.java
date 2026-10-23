package com.catedra.catalogo.service;

import com.catedra.catalogo.domain.Category;
import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import com.catedra.catalogo.domain.CatalogVersion;

import java.util.List;
import java.util.Optional;

public interface CatalogService {
    List<Category> getActiveCategories();
    List<Professional> getActiveProfessionals();
    List<Professional> getProfessionalsByCategory(Long categoryId);
    List<WeeklySchedule> getSchedulesByProfessional(Long professionalId);
    Optional<CatalogVersion> getCurrentVersion();
}