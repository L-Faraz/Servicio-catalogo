package com.catedra.catalogo.listener;

import com.catedra.catalogo.config.CatedraProperties;
import com.catedra.catalogo.domain.ProcessedCatalogEvent;
import com.catedra.catalogo.dto.CatalogUpdatedEventDTO;
import com.catedra.catalogo.repository.ProcessedCatalogEventRepository;
import com.catedra.catalogo.service.SyncService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "catedra.integration.kafka-enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnExpression("'${catedra.integration.group-id:}' != '' || '${catedra.integration.kafka-catalog-topic:}' != ''")
public class CatalogEventListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogEventListener.class);

    private final SyncService syncService;
    private final ProcessedCatalogEventRepository processedEventRepository;
    private final CatedraProperties properties;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public CatalogEventListener(SyncService syncService,
                                 ProcessedCatalogEventRepository processedEventRepository,
                                 CatedraProperties properties,
                                 ObjectMapper objectMapper,
                                 MeterRegistry meterRegistry) {
        this.syncService = syncService;
        this.processedEventRepository = processedEventRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(
            topics = "#{@catedraProperties.effectiveKafkaCatalogTopic}",
            groupId = "#{@catedraProperties.effectiveKafkaConsumerGroupId}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCatalogEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        CatalogUpdatedEventDTO event = parse(record.value());
        if (event == null) {
            count("invalid");
            acknowledgment.acknowledge();
            return;
        }

        if (!"CatalogUpdated".equals(event.eventType())
                || event.schemaVersion() == null
                || event.schemaVersion() != 1
                || event.newVersion() == null) {
            log.warn("Evento de catalogo ignorado por tipo, esquema o nueva version invalidos: {}", record.value());
            count("invalid");
            acknowledgment.acknowledge();
            return;
        }

        if (!StringUtils.hasText(event.eventId())) {
            log.error("Evento CatalogUpdated sin eventId. Se ignora: {}", record.value());
            count("invalid");
            acknowledgment.acknowledge();
            return;
        }

        if (properties.isProvisioningBlocked()) {
            log.warn("Integracion no PROVISIONED ({}). Evento {} ignorado sin sincronizar.",
                    properties.getProvisioningStatus(), event.eventId());
            count("blocked");
            acknowledgment.acknowledge();
            return;
        }

        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Evento {} ya procesado. Se omite.", event.eventId());
            count("duplicate");
            acknowledgment.acknowledge();
            return;
        }

        try {
            syncService.processIncrementalSync();
            if (!syncService.isVersionApplied(event.newVersion())) {
                throw new IllegalStateException("La sincronizacion no alcanzo la version "
                        + event.newVersion() + " del evento " + event.eventId());
            }
            markProcessed(event.eventId());
            count("processed");
            acknowledgment.acknowledge();
        } catch (RuntimeException e) {
            log.error("Fallo el procesamiento del evento {}. No se confirma el offset.", event.eventId(), e);
            count("failed");
            throw e;
        }
    }

    private void count(String outcome) {
        meterRegistry.counter("catalog.kafka.events", "outcome", outcome).increment();
    }

    private void markProcessed(String eventId) {
        try {
            processedEventRepository.save(new ProcessedCatalogEvent(eventId));
        } catch (DataIntegrityViolationException e) {
            log.info("Evento {} ya fue registrado concurrentemente.", eventId);
        }
    }

    private CatalogUpdatedEventDTO parse(String payload) {
        try {
            return objectMapper.readValue(payload, CatalogUpdatedEventDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Evento Kafka invalido: {}", payload, e);
            return null;
        }
    }
}
