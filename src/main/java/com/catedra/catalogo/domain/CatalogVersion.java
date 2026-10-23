package com.catedra.catalogo.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_versions")
public class CatalogVersion {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private Long currentVersion;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    public CatalogVersion() {}

    public CatalogVersion(Long currentVersion) {
        this.id = 1L;
        this.currentVersion = currentVersion;
        this.lastSyncedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(Long currentVersion) { this.currentVersion = currentVersion; }
    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}