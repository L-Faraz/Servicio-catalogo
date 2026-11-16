package com.catedra.catalogo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogSnapshotDTO(
        Long snapshotVersion,
        Instant generatedAt,
        List<CategorySnapshotDTO> professionalCategories,
        List<ProfessionalSnapshotDTO> professionals,
        List<WeeklyScheduleSnapshotDTO> weeklySchedules
) {
}
