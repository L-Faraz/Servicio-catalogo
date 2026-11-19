package com.catedra.catalogo.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.catedra.catalogo.config.CatedraProperties;
import com.catedra.catalogo.exception.IntegrationNotConfiguredException;
import com.catedra.catalogo.service.SyncService;
import org.junit.jupiter.api.Test;

class SyncControllerTest {

    @Test
    void blocksSnapshotWhenProvisioningIsBlocked() {
        SyncService syncService = mock(SyncService.class);
        CatedraProperties properties = mock(CatedraProperties.class);
        when(properties.isProvisioningBlocked()).thenReturn(true);
        SyncController controller = new SyncController(syncService, properties);

        assertThatThrownBy(controller::syncSnapshot)
                .isInstanceOf(IntegrationNotConfiguredException.class);

        verifyNoInteractions(syncService);
    }

    @Test
    void blocksIncrementalWhenProvisioningIsBlocked() {
        SyncService syncService = mock(SyncService.class);
        CatedraProperties properties = mock(CatedraProperties.class);
        when(properties.isProvisioningBlocked()).thenReturn(true);
        SyncController controller = new SyncController(syncService, properties);

        assertThatThrownBy(controller::syncIncremental)
                .isInstanceOf(IntegrationNotConfiguredException.class);

        verifyNoInteractions(syncService);
    }

    @Test
    void allowsSnapshotWhenProvisioningIsReady() {
        SyncService syncService = mock(SyncService.class);
        CatedraProperties properties = mock(CatedraProperties.class);
        when(properties.isProvisioningBlocked()).thenReturn(false);
        SyncController controller = new SyncController(syncService, properties);

        controller.syncSnapshot();

        verify(syncService).syncSnapshot();
    }

    @Test
    void allowsIncrementalWhenProvisioningIsReady() {
        SyncService syncService = mock(SyncService.class);
        CatedraProperties properties = mock(CatedraProperties.class);
        when(properties.isProvisioningBlocked()).thenReturn(false);
        SyncController controller = new SyncController(syncService, properties);

        controller.syncIncremental();

        verify(syncService).processIncrementalSync();
    }
}
