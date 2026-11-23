package com.catedra.catalogo.config;

import com.catedra.catalogo.service.SyncService;
import com.catedra.catalogo.service.SyncStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncBootstrapTest {

    @Mock
    private SyncService syncService;

    @Mock
    private SyncStatusService statusService;

    @Test
    void skipsInitialSnapshotWhileProvisioningIsNotCompleted() {
        CatedraProperties properties = configuredProperties();
        properties.setProvisioningStatus("PENDING");

        new SyncBootstrap(syncService, statusService, properties).run(null);

        verify(statusService).recoverInterrupted();
        verify(syncService, never()).syncSnapshot();
        verify(syncService, never()).isLocalCopyConsistent();
    }

    @Test
    void runsInitialSnapshotWhenProvisionedAndLocalCopyIsIncomplete() {
        CatedraProperties properties = configuredProperties();
        properties.setProvisioningStatus("PROVISIONED");
        when(syncService.isLocalCopyConsistent()).thenReturn(false);

        new SyncBootstrap(syncService, statusService, properties).run(null);

        verify(syncService).syncSnapshot();
    }

    @Test
    void skipsInitialSnapshotWhenLocalCopyIsComplete() {
        CatedraProperties properties = configuredProperties();
        when(syncService.isLocalCopyConsistent()).thenReturn(true);

        new SyncBootstrap(syncService, statusService, properties).run(null);

        verify(syncService, never()).syncSnapshot();
    }

    @Test
    void doesNotRequireRedisToRunTheInitialSnapshot() {
        CatedraProperties properties = configuredProperties();
        properties.setRedisHost(null);
        when(syncService.isLocalCopyConsistent()).thenReturn(false);

        new SyncBootstrap(syncService, statusService, properties).run(null);

        verify(syncService).syncSnapshot();
    }

    @Test
    void skipsSnapshotWhenApiConfigurationIsMissing() {
        CatedraProperties properties = configuredProperties();
        properties.setApiBaseUrl(null);

        new SyncBootstrap(syncService, statusService, properties).run(null);

        verify(syncService, never()).syncSnapshot();
    }

    private CatedraProperties configuredProperties() {
        CatedraProperties properties = new CatedraProperties();
        properties.setApiBaseUrl("http://localhost:9999");
        properties.setJwtToken("test-token");
        properties.setGroupId("test-group");
        properties.setRedisHost("localhost");
        return properties;
    }
}
