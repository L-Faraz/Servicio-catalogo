package com.catedra.catalogo.service;

import com.catedra.catalogo.domain.Category;
import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import com.catedra.catalogo.dto.CategoryDTO;
import com.catedra.catalogo.dto.ProfessionalDTO;
import com.catedra.catalogo.dto.ProfessionalDetailDTO;
import com.catedra.catalogo.dto.WeeklyScheduleDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogMapper {

    public CategoryDTO toCategoryDto(Category category) {
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getEnabled(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public WeeklyScheduleDTO toWeeklyScheduleDto(WeeklySchedule schedule) {
        return new WeeklyScheduleDTO(
                schedule.getId(),
                schedule.getProfessional() != null ? schedule.getProfessional().getId() : null,
                schedule.getDayOfWeek() != null ? schedule.getDayOfWeek().name() : null,
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getSlotDurationMinutes(),
                schedule.getEnabled(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }

    public ProfessionalDTO toProfessionalDto(Professional professional) {
        return new ProfessionalDTO(
                professional.getId(),
                professional.getFirstName(),
                professional.getLastName(),
                professional.getCategory() != null ? professional.getCategory().getId() : null,
                professional.getCategory() != null ? professional.getCategory().getName() : null,
                professional.getEnabled(),
                professional.getCreatedAt(),
                professional.getUpdatedAt()
        );
    }

    public ProfessionalDetailDTO toProfessionalDetailDto(Professional professional, List<WeeklySchedule> schedules) {
        return new ProfessionalDetailDTO(
                professional.getId(),
                professional.getFirstName(),
                professional.getLastName(),
                professional.getCategory() != null ? professional.getCategory().getId() : null,
                professional.getCategory() != null ? professional.getCategory().getName() : null,
                professional.getEnabled(),
                professional.getCreatedAt(),
                professional.getUpdatedAt(),
                schedules.stream().map(this::toWeeklyScheduleDto).toList()
        );
    }
}
