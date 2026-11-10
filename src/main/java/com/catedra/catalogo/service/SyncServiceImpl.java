package com.catedra.catalogo.service;

import com.catedra.catalogo.client.CatedraApiClient;
import com.catedra.catalogo.domain.CatalogVersion;
import com.catedra.catalogo.domain.Category;
import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import com.catedra.catalogo.dto.*;
import com.catedra.catalogo.repository.CatalogVersionRepository;
import com.catedra.catalogo.repository.CategoryRepository;
import com.catedra.catalogo.repository.ProfessionalRepository;
import com.catedra.catalogo.repository.RedisSyncRepository;
import com.catedra.catalogo.repository.WeeklyScheduleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SyncServiceImpl implements SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncServiceImpl.class);

    private final CatedraApiClient apiClient;
    private final RedisSyncRepository redisRepository;
    private final CatalogVersionRepository versionRepository;
    private final CategoryRepository categoryRepository;
    private final ProfessionalRepository professionalRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final ObjectMapper objectMapper;

    public SyncServiceImpl(CatedraApiClient apiClient,
                           RedisSyncRepository redisRepository,
                           CatalogVersionRepository versionRepository,
                           CategoryRepository categoryRepository,
                           ProfessionalRepository professionalRepository,
                           WeeklyScheduleRepository weeklyScheduleRepository,
                           ObjectMapper objectMapper) {
        this.apiClient = apiClient;
        this.redisRepository = redisRepository;
        this.versionRepository = versionRepository;
        this.categoryRepository = categoryRepository;
        this.professionalRepository = professionalRepository;
        this.weeklyScheduleRepository = weeklyScheduleRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public CatalogSnapshotDTO syncSnapshot() {
        log.info("Iniciando sincronización por Snapshot...");
        return new CatalogSnapshotDTO(0L, List.of(), List.of(), List.of());
    }

    @Override
    public void processIncrementalSync() {
        log.info("Iniciando proceso de sincronización incremental...");
    }

    @Transactional
    public void applyChange(String changeJson) {
        try {
            CatalogChangeEventDTO event = objectMapper.readValue(changeJson, CatalogChangeEventDTO.class);
            log.info("Procesando mutación de catálogo: {} | Acción: {} | Versión: {}",
                    event.entityType(), event.action(), event.version());

            switch (event.entityType().toUpperCase()) {
                case "CATEGORY" -> processCategoryChange(event);
                case "PROFESSIONAL" -> processProfessionalChange(event);
                case "WEEKLY_SCHEDULE" -> processWeeklyScheduleChange(event);
                default -> log.warn("Tipo de entidad desconocido: {}", event.entityType());
            }

            updateLocalVersion(event.version());

        } catch (JsonProcessingException e) {
            log.error("Error al deserializar evento JSON: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error inesperado al aplicar cambio: {}", e.getMessage(), e);
        }
    }

    private void processCategoryChange(CatalogChangeEventDTO event) throws JsonProcessingException {
        CategoryDTO dto = objectMapper.readValue(event.payloadJson(), CategoryDTO.class);
        String action = event.action().toUpperCase();

        if ("DELETE".equals(action)) {
            categoryRepository.findById(dto.getId()).ifPresent(category -> {
                category.setEnabled(false);
                categoryRepository.save(category);
            });
            log.debug("Category borrada (deshabilitada) con ID: {}", dto.getId());
        } else {
            Category category = categoryRepository.findById(dto.getId())
                    .orElseGet(Category::new);
            category.setId(dto.getId());
            category.setName(dto.getName());
            category.setEnabled(true);
            categoryRepository.save(category);
            log.debug("Category guardada/actualizada con ID: {}", dto.getId());
        }
    }

    private void processProfessionalChange(CatalogChangeEventDTO event) throws JsonProcessingException {
        ProfessionalDTO dto = objectMapper.readValue(event.payloadJson(), ProfessionalDTO.class);
        String action = event.action().toUpperCase();

        if ("DELETE".equals(action)) {
            professionalRepository.findById(dto.getId()).ifPresent(prof -> {
                prof.setEnabled(false);
                professionalRepository.save(prof);
            });
            log.debug("Professional borrado (deshabilitado) con ID: {}", dto.getId());
        } else {
            Professional professional = professionalRepository.findById(dto.getId())
                    .orElseGet(Professional::new);
            professional.setId(dto.getId());
            professional.setName(dto.getFirstName());
            professional.setLastName(dto.getLastName());
            professional.setEmail(dto.getEmail());
            professional.setEnabled(dto.isActive());

            if (dto.getCategory() != null && dto.getCategory().getId() != null) {
                Category category = categoryRepository.findById(dto.getCategory().getId())
                        .orElse(null);
                professional.setCategory(category);
            }

            professionalRepository.save(professional);
            log.debug("Professional guardado/actualizado con ID: {}", dto.getId());
        }
    }

    private void processWeeklyScheduleChange(CatalogChangeEventDTO event) throws JsonProcessingException {
        WeeklyScheduleDTO dto = objectMapper.readValue(event.payloadJson(), WeeklyScheduleDTO.class);
        String action = event.action().toUpperCase();

        if ("DELETE".equals(action)) {
            weeklyScheduleRepository.findById(dto.getId()).ifPresent(schedule -> {
                schedule.setEnabled(false);
                weeklyScheduleRepository.save(schedule);
            });
            log.debug("WeeklySchedule borrado (deshabilitado) con ID: {}", dto.getId());
        } else {
            WeeklySchedule schedule = weeklyScheduleRepository.findById(dto.getId())
                    .orElseGet(WeeklySchedule::new);
            schedule.setId(dto.getId());
            if (dto.getDayOfWeek() != null) {
                schedule.setDayOfWeek(dto.getDayOfWeek().getValue());
            }
            schedule.setStartTime(dto.getStartTime());
            schedule.setEndTime(dto.getEndTime());
            schedule.setEnabled(true);

            weeklyScheduleRepository.save(schedule);
            log.debug("WeeklySchedule guardado/actualizado con ID: {}", dto.getId());
        }
    }

    private void updateLocalVersion(Long newVersion) {
        CatalogVersion version = versionRepository.findById(1L)
                .orElseGet(() -> new CatalogVersion(0L));

        version.setCurrentVersion(newVersion);
        version.setLastSyncedAt(LocalDateTime.now());
        versionRepository.save(version);
        log.debug("Versión local actualizada a: {}", newVersion);
    }
}