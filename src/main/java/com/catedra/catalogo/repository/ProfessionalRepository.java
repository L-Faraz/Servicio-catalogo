package com.catedra.catalogo.repository;

import com.catedra.catalogo.domain.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfessionalRepository extends JpaRepository<Professional, Long> {
    List<Professional> findByEnabledTrue();
    List<Professional> findByCategoryIdAndEnabledTrue(Long categoryId);
}