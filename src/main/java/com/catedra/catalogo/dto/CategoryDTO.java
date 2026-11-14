package com.catedra.catalogo.dto;

import java.time.Instant;

public record CategoryDTO(
        Long id,
        String name,
        String description,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
