package com.catedra.catalogo.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.catedra.catalogo.config.CatedraProperties;
import com.catedra.catalogo.repository.ProcessedCatalogEventRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProcessedCatalogEventsCleanupJobTest {

    @Test
    void deletesEventsOlderThanRetention() {
        ProcessedCatalogEventRepository repository = mock(ProcessedCatalogEventRepository.class);
        CatedraProperties properties = mock(CatedraProperties.class);
        when(properties.getProcessedEventsRetentionDays()).thenReturn(7L);
        ProcessedCatalogEventsCleanupJob job = new ProcessedCatalogEventsCleanupJob(repository, properties);

        Instant before = Instant.now().minus(Duration.ofDays(7));
        job.cleanup();
        Instant after = Instant.now().minus(Duration.ofDays(7));

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteByProcessedAtBefore(cutoff.capture());
        assertThat(cutoff.getValue()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void skipsCleanupWhenRetentionIsNotPositive() {
        ProcessedCatalogEventRepository repository = mock(ProcessedCatalogEventRepository.class);
        CatedraProperties properties = mock(CatedraProperties.class);
        when(properties.getProcessedEventsRetentionDays()).thenReturn(0L);
        ProcessedCatalogEventsCleanupJob job = new ProcessedCatalogEventsCleanupJob(repository, properties);

        job.cleanup();

        verifyNoInteractions(repository);
    }
}
