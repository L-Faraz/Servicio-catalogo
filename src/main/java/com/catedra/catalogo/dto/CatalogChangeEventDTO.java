package com.catedra.catalogo.dto;

import java.time.LocalDateTime;

public record CatalogChangeEventDTO(
        Long version,
        String entityType,  // "CATEGORY", "PROFESSIONAL", "WEEKLY_SCHEDULE"
        String action,      // "CREATE", "UPDATE", "DELETE"
        String payloadJson, // Objeto JSON serializado con los datos de la entidad
        LocalDateTime timestamp
) {}