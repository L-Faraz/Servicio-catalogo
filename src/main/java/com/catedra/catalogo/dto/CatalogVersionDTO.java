package com.catedra.catalogo.dto;

import java.time.Instant;

public record CatalogVersionDTO(
        Long localVersion,
        Instant lastSyncedAt
) {
}
