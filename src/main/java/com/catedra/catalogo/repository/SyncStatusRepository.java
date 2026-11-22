package com.catedra.catalogo.repository;

import com.catedra.catalogo.domain.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncStatusRepository extends JpaRepository<SyncStatus, Long> {
}
