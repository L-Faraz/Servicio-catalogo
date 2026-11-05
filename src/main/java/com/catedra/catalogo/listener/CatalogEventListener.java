package com.catedra.catalogo.listener;

import com.catedra.catalogo.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CatalogEventListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogEventListener.class);

    private final SyncService syncService;

    public CatalogEventListener(SyncService syncService) {
        this.syncService = syncService;
    }

    @KafkaListener(
            topics = "${catedra.integration.kafka-topic:catedra.catalog.events}",
            groupId = "${catedra.integration.kafka-group-id:servicio-catalogo-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCatalogEvent(String eventJson) {
        log.info("Evento recibido de Kafka: {}", eventJson);

        try {
            // Al recibir un evento en tiempo real, disparamos la verificación/sincronización incremental
            syncService.processIncrementalSync();
        } catch (Exception e) {
            log.error("Error al procesar el evento de Kafka: {}", e.getMessage(), e);
        }
    }
}