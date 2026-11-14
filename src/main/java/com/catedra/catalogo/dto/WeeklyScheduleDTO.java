package com.catedra.catalogo.dto;

import java.time.Instant;
import java.time.LocalTime;

public record WeeklyScheduleDTO(
        Long id,
        Long professionalId,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
