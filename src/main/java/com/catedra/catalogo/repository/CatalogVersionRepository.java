package com.catedra.catalogo.repository;

import com.catedra.catalogo.domain.CatalogVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CatalogVersionRepository extends JpaRepository<CatalogVersion, Long> {
    Optional<CatalogVersion> findTopByOrderByVersionNumberDesc();
}