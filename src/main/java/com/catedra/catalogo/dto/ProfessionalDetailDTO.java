package com.catedra.catalogo.dto;

import java.time.Instant;
import java.util.List;

public record ProfessionalDetailDTO(
        Long id,
        String firstName,
        String lastName,
        Long categoryId,
        String categoryName,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        List<WeeklyScheduleDTO> schedules
) {
}
