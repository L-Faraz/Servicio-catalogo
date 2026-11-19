package com.catedra.catalogo.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.catedra.catalogo.config.CatedraProperties;
import com.catedra.catalogo.service.SyncService;
import org.junit.jupiter.api.Test;

class CatalogReconciliationJobTest {

    @Test
    void skipsReconciliationWhenProvisioningIsBlocked() {
        SyncService syncService = mock(SyncService.class);
        CatedraProperties properties = mock(CatedraProperties.class);
        when(properties.isConfigured()).thenReturn(true);
        when(properties.isProvisioningBlocked()).thenReturn(true);
        CatalogReconciliationJob job = new CatalogReconciliationJob(syncService, properties);

        job.reconcile();

        verifyNoInteractions(syncService);
    }

    @Test
    void skipsReconciliationWhenIntegrationIsNotConfigured() {
        SyncService syncService = mock(SyncService.class);
        CatedraProperties properties = mock(CatedraProperties.class);
        when(properties.isConfigured()).thenReturn(false);
        CatalogReconciliationJob job = new CatalogReconciliationJob(syncService, properties);

        job.reconcile();

        verifyNoInteractions(syncService);
    }

    @Test
    void runsReconciliationWhenReady() {
        SyncService syncService = mock(SyncService.class);
        CatedraProperties properties = mock(CatedraProperties.class);
        when(properties.isConfigured()).thenReturn(true);
        when(properties.isProvisioningBlocked()).thenReturn(false);
        CatalogReconciliationJob job = new CatalogReconciliationJob(syncService, properties);

        job.reconcile();

        verify(syncService).processIncrementalSync();
    }
}
