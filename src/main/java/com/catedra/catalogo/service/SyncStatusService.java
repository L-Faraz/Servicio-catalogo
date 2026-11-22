package com.catedra.catalogo.service;

import com.catedra.catalogo.domain.SyncStatus;
import com.catedra.catalogo.repository.SyncStatusRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SyncStatusService {

    static final String IDLE = "IDLE";
    static final String SYNCING = "SYNCING";
    static final String ERROR = "ERROR";

    private final SyncStatusRepository repository;

    public SyncStatusService(SyncStatusRepository repository) {
        this.repository = repository;
    }

    public void markSyncing() {
        update(SYNCING, null);
    }

    public void markIdle() {
        update(IDLE, null);
    }

    public void markError(String message) {
        update(ERROR, message);
    }

    public void recoverInterrupted() {
        SyncStatus status = current();
        if (SYNCING.equals(status.getState())) {
            update(ERROR, "La sincronizacion fue interrumpida por un reinicio");
        }
    }

    public String getState() {
        return current().getState();
    }

    public String getLastError() {
        return current().getLastError();
    }

    private void update(String state, String lastError) {
        SyncStatus status = current();
        status.setState(state);
        status.setLastError(truncate(lastError));
        status.setUpdatedAt(Instant.now());
        repository.save(status);
    }

    private SyncStatus current() {
        return repository.findById(1L).orElseGet(SyncStatus::new);
    }

    private String truncate(String message) {
        if (message == null || message.length() <= 2000) {
            return message;
        }
        return message.substring(0, 2000);
    }
}
