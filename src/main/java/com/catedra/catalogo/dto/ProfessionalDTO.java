package com.catedra.catalogo.dto;

import java.time.Instant;

public record ProfessionalDTO(
        Long id,
        String firstName,
        String lastName,
        Long categoryId,
        String categoryName,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
