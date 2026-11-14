package com.catedra.catalogo.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class SyncStatusService {

    private final AtomicReference<String> state = new AtomicReference<>("IDLE");
    private final AtomicReference<String> lastError = new AtomicReference<>();

    public void markSyncing() {
        lastError.set(null);
        state.set("SYNCING");
    }

    public void markIdle() {
        state.set("IDLE");
    }

    public void markError(String message) {
        lastError.set(message);
        state.set("ERROR");
    }

    public String getState() {
        return state.get();
    }

    public String getLastError() {
        return lastError.get();
    }
}
