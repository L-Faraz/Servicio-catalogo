package com.catedra.catalogo.dto;

import java.time.LocalTime;

public record WeeklyScheduleSnapshotDTO(
        Long id,
        Integer dayOfWeek,           // El día de la semana (ej: 1 para Lunes, 2 para Martes)
        LocalTime startTime,         // A qué hora empieza a atender (ej: 08:00)
        LocalTime endTime,           // A qué hora termina (ej: 12:00)
        Integer slotDurationMinutes, // Cuánto dura cada turno en minutos (ej: 30)
        Long professionalId,         // El ID del profesional dueño de este horario
        Boolean enabled
) {}