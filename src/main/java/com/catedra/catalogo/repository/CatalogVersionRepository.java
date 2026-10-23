package com.catedra.catalogo.repository;

import com.catedra.catalogo.domain.CatalogVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogVersionRepository extends JpaRepository<CatalogVersion, Long> {
}