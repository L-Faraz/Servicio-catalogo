package com.catedra.catalogo.dto;

import java.util.List;

public record CatalogSnapshotDTO(
        Long snapshotVersion,                               // Un número de versión para saber qué tan reciente es la info
        List<CategorySnapshotDTO> categories,               // La lista con todas las categorías
        List<ProfessionalSnapshotDTO> professionals,        // La lista con todos los profesionales
        List<WeeklyScheduleSnapshotDTO> weeklySchedules     // La lista con todos los horarios
) {}