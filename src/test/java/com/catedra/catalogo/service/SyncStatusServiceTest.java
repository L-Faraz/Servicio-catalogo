package com.catedra.catalogo.service;

import com.catedra.catalogo.domain.SyncStatus;
import com.catedra.catalogo.repository.SyncStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncStatusServiceTest {

    @Mock
    private SyncStatusRepository repository;

    @Test
    void marksErrorAndPersistsTruncatedMessage() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        SyncStatusService service = new SyncStatusService(repository);

        service.markError("x".repeat(2500));

        ArgumentCaptor<SyncStatus> captor = ArgumentCaptor.forClass(SyncStatus.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo("ERROR");
        assertThat(captor.getValue().getLastError()).hasSize(2000);
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void recoversInterruptedSyncOnStartup() {
        SyncStatus interrupted = new SyncStatus();
        interrupted.setState("SYNCING");
        when(repository.findById(1L)).thenReturn(Optional.of(interrupted));
        SyncStatusService service = new SyncStatusService(repository);

        service.recoverInterrupted();

        verify(repository).save(any(SyncStatus.class));
        assertThat(interrupted.getState()).isEqualTo("ERROR");
        assertThat(interrupted.getLastError()).contains("reinicio");
    }

    @Test
    void readsPersistedStateInsteadOfMemory() {
        SyncStatus persisted = new SyncStatus();
        persisted.setState("IDLE");
        when(repository.findById(1L)).thenReturn(Optional.of(persisted));
        SyncStatusService service = new SyncStatusService(repository);

        service.markSyncing();

        assertThat(persisted.getState()).isEqualTo("SYNCING");
        assertThat(persisted.getLastError()).isNull();
    }
}
