package com.catedra.catalogo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sync_status")
public class SyncStatus {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private String state = "IDLE";

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public SyncStatus() {
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
