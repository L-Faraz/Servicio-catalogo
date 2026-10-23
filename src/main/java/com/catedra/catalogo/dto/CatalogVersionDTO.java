package com.catedra.catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogVersionDTO {
    private Long id;
    private Long versionNumber;
    private LocalDateTime updatedAt;
    private String description;
}