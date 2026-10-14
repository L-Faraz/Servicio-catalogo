package com.catedra.catalogo.service;

import com.catedra.catalogo.dto.CategoryDTO;
import com.catedra.catalogo.dto.ProfessionalDTO;
import com.catedra.catalogo.dto.CatalogVersionDTO;

import java.util.List;

public interface CatalogService {
    List<CategoryDTO> getAllCategories();
    CategoryDTO createCategory(CategoryDTO categoryDTO);
    List<ProfessionalDTO> getAllActiveProfessionals();
    List<ProfessionalDTO> getProfessionalsByCategory(Long categoryId);
    ProfessionalDTO createProfessional(ProfessionalDTO professionalDTO);
    CatalogVersionDTO getCurrentCatalogVersion();
    CatalogVersionDTO incrementCatalogVersion(String description);
}