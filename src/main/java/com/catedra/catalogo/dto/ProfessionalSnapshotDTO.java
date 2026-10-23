package com.catedra.catalogo.dto;

public record ProfessionalSnapshotDTO(
        Long id,
        String name,
        String lastName,
        String email,
        Long categoryId,
        Boolean enabled
) {}