package com.catedra.catalogo.dto;

import java.time.Instant;

public record SyncStatusDTO(
        Long localVersion,
        Long currentVersion,
        Long oldestAvailableVersion,
        Instant lastSyncedAt,
        String lastError,
        String state
) {
}
