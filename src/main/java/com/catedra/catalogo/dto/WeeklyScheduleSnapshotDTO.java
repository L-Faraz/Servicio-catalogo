package com.catedra.catalogo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeeklyScheduleSnapshotDTO(
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
