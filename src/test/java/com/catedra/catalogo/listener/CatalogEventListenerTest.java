package com.catedra.catalogo.listener;

import com.catedra.catalogo.domain.ProcessedCatalogEvent;
import com.catedra.catalogo.repository.ProcessedCatalogEventRepository;
import com.catedra.catalogo.service.SyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogEventListenerTest {

    private static final String VALID_EVENT = """
            {"eventId":"catalog-event-7","eventType":"CatalogUpdated","newVersion":7,
             "occurredAt":"2026-09-10T13:44:59Z","source":"MANUAL","schemaVersion":1}
            """;

    @Mock
    private SyncService syncService;
    @Mock
    private ProcessedCatalogEventRepository processedEventRepository;
    @Mock
    private Acknowledgment acknowledgment;

    private CatalogEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new CatalogEventListener(
                syncService,
                processedEventRepository,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void validEventSynchronizesAndAcknowledges() {
        when(processedEventRepository.existsById("catalog-event-7")).thenReturn(false);

        listener.handleCatalogEvent(record(VALID_EVENT), acknowledgment);

        verify(syncService).processIncrementalSync();
        verify(processedEventRepository).save(any(ProcessedCatalogEvent.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void duplicateEventDoesNotSynchronize() {
        when(processedEventRepository.existsById("catalog-event-7")).thenReturn(true);

        listener.handleCatalogEvent(record(VALID_EVENT), acknowledgment);

        verify(syncService, never()).processIncrementalSync();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void eventWithUnexpectedTypeIsIgnored() {
        String payload = """
                {"eventId":"other","eventType":"OtherEvent","newVersion":7,"schemaVersion":1}
                """;

        listener.handleCatalogEvent(record(payload), acknowledgment);

        verify(syncService, never()).processIncrementalSync();
        verify(processedEventRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void invalidJsonIsIgnoredAndAcknowledged() {
        listener.handleCatalogEvent(record("{not-json"), acknowledgment);

        verify(syncService, never()).processIncrementalSync();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void syncFailureDoesNotAcknowledge() {
        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        org.mockito.Mockito.doThrow(new IllegalStateException("sync failed"))
                .when(syncService).processIncrementalSync();

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> listener.handleCatalogEvent(record(VALID_EVENT), acknowledgment))
                .isInstanceOf(IllegalStateException.class);

        verify(acknowledgment, never()).acknowledge();
        verify(processedEventRepository, never()).save(any());
    }

    private ConsumerRecord<String, String> record(String payload) {
        return new ConsumerRecord<>("catedra.catalog.test-group", 0, 0L, "7", payload);
    }
}
