package com.catedra.catalogo.repository;

import com.catedra.catalogo.domain.ProcessedCatalogEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedCatalogEventRepository extends JpaRepository<ProcessedCatalogEvent, String> {
}
