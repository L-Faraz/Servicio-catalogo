package com.catedra.catalogo.service;

import com.catedra.catalogo.domain.CatalogVersion;
import com.catedra.catalogo.dto.CategoryDTO;
import com.catedra.catalogo.dto.ProfessionalDTO;
import com.catedra.catalogo.dto.ProfessionalDetailDTO;
import com.catedra.catalogo.dto.WeeklyScheduleDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface CatalogService {

    List<CategoryDTO> getCategories(Boolean enabled);

    Page<ProfessionalDTO> searchProfessionals(Long categoryId,
                                              String name,
                                              Boolean enabled,
                                              DayOfWeek dayOfWeek,
                                              LocalTime time,
                                              Pageable pageable);

    ProfessionalDetailDTO getProfessional(Long id);

    List<WeeklyScheduleDTO> getSchedulesByProfessional(Long professionalId);

    Optional<CatalogVersion> getCurrentVersion();
}
