package com.catedra.catalogo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "catalog_versions")
public class CatalogVersion {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private Long currentVersion;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    public CatalogVersion() {
    }

    public CatalogVersion(Long currentVersion) {
        this.id = 1L;
        this.currentVersion = currentVersion;
        this.lastSyncedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(Long currentVersion) { this.currentVersion = currentVersion; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
