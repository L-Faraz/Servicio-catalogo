package com.catedra.catalogo.dto;

public record CategorySnapshotDTO(
        Long id,
        String name,
        Boolean enabled
) {}