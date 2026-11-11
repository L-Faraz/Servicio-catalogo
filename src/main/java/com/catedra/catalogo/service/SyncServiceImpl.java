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

    private final CatedraApiClient catedraApiClient;
    private final CategoryRepository categoryRepository;
    private final ProfessionalRepository professionalRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final RedisSyncRepository redisSyncRepository;
    private final CatalogVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    public SyncServiceImpl(
            CatedraApiClient catedraApiClient,
            CategoryRepository categoryRepository,
            ProfessionalRepository professionalRepository,
            WeeklyScheduleRepository weeklyScheduleRepository,
            RedisSyncRepository redisSyncRepository,
            CatalogVersionRepository versionRepository,
            ObjectMapper objectMapper) {
        this.catedraApiClient = catedraApiClient;
        this.categoryRepository = categoryRepository;
        this.professionalRepository = professionalRepository;
        this.weeklyScheduleRepository = weeklyScheduleRepository;
        this.redisSyncRepository = redisSyncRepository;
        this.versionRepository = versionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public CatalogSnapshotDTO syncSnapshot() {
        log.info("Iniciando sincronización completa por Snapshot...");
        CatalogSnapshotDTO snapshot = catedraApiClient.fetchSnapshot();

        if (snapshot == null) {
            log.error("El snapshot recibido de la API es nulo.");
            return null;
        }

        Long newVersion = snapshot.snapshotVersion() != null ? snapshot.snapshotVersion() : redisSyncRepository.getCurrentVersion();
        if (newVersion != null) {
            updateLocalVersion(newVersion);
        }

        return snapshot;
    }

    @Override
    @Transactional
    public void processIncrementalSync() {
        Long redisVersion = redisSyncRepository.getCurrentVersion();
        Long oldestVersion = redisSyncRepository.getOldestAvailableVersion();

        if (redisVersion == null) {
            log.warn("No hay información de versiones en Redis. Cancelando proceso incremental.");
            return;
        }

        Long localVersion = versionRepository.findById(1L)
                .map(CatalogVersion::getCurrentVersion)
                .orElse(0L);

        if (localVersion.equals(redisVersion)) {
            log.debug("El catálogo local ya está en la última versión ({}).", localVersion);
            return;
        }

        // Fallback: Si estamos más atrasados que la versión más vieja disponible en Redis
        if (oldestVersion != null && localVersion < (oldestVersion - 1)) {
            log.warn("Versión local ({}) está por detrás de la más antigua en Redis ({}). Ejecutando Snapshot de Fallback...", localVersion, oldestVersion);
            syncSnapshot();
            return;
        }

        // Procesamiento incremental en lote usando el método getEventsBetween de RedisSyncRepository
        Long nextVersion = localVersion + 1;
        log.info("Recuperando eventos incrementales desde la versión {} hasta {}", nextVersion, redisVersion);

        List<String> missingEvents = redisSyncRepository.getEventsBetween(nextVersion, redisVersion);
        for (String eventJson : missingEvents) {
            applyChange(eventJson);
        }

        log.info("Sincronización incremental finalizada.");
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
        CatalogVersion version = versionRepository.findById(1L).orElseGet(() -> new CatalogVersion(0L));
        version.setCurrentVersion(newVersion);
        version.setLastSyncedAt(LocalDateTime.now());
        versionRepository.save(version);
        log.debug("Versión local actualizada a: {}", newVersion);
    }
}