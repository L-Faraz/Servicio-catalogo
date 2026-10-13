package com.catedra.catalogo.repository;

import com.catedra.catalogo.domain.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, Long> {
    Optional<Professional> findByEmail(String email);
    List<Professional> findByCategoryId(Long categoryId);
    List<Professional> findByActiveTrue();
}