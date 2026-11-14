package com.catedra.catalogo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogUpdatedEventDTO(
        String eventId,
        String eventType,
        Long newVersion,
        Instant occurredAt,
        String source,
        Integer schemaVersion
) {
}
