package com.catedra.catalogo.config;

import com.catedra.catalogo.repository.CatalogVersionRepository;
import com.catedra.catalogo.service.SyncService;
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
    private CatalogVersionRepository versionRepository;

    @Test
    void skipsInitialSnapshotWhileProvisioningIsNotCompleted() {
        CatedraProperties properties = configuredProperties();
        properties.setProvisioningStatus("PENDING");

        new SyncBootstrap(syncService, versionRepository, properties).run(null);

        verify(syncService, never()).syncSnapshot();
        verify(versionRepository, never()).count();
    }

    @Test
    void runsInitialSnapshotWhenProvisionedAndLocalDatabaseIsEmpty() {
        CatedraProperties properties = configuredProperties();
        properties.setProvisioningStatus("PROVISIONED");
        when(versionRepository.count()).thenReturn(0L);

        new SyncBootstrap(syncService, versionRepository, properties).run(null);

        verify(syncService).syncSnapshot();
    }

    @Test
    void runsInitialSnapshotWhenProvisioningStatusIsNotInformed() {
        CatedraProperties properties = configuredProperties();
        when(versionRepository.count()).thenReturn(0L);

        new SyncBootstrap(syncService, versionRepository, properties).run(null);

        verify(syncService).syncSnapshot();
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
