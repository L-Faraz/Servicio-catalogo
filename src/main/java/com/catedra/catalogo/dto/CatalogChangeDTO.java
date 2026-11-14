package com.catedra.catalogo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogChangeDTO(
        Long version,
        ChangeLists changes
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChangeLists(
            List<Long> professionalCategories,
            List<Long> professionals,
            List<Long> weeklySchedules
    ) {
    }
}
