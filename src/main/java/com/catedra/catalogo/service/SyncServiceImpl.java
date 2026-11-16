package com.catedra.catalogo.service;

import com.catedra.catalogo.client.CatedraApiClient;
import com.catedra.catalogo.domain.CatalogVersion;
import com.catedra.catalogo.domain.Category;
import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import com.catedra.catalogo.dto.CatalogChangeDTO;
import com.catedra.catalogo.dto.CatalogSnapshotDTO;
import com.catedra.catalogo.dto.CategorySnapshotDTO;
import com.catedra.catalogo.dto.ProfessionalSnapshotDTO;
import com.catedra.catalogo.dto.SyncStatusDTO;
import com.catedra.catalogo.dto.WeeklyScheduleSnapshotDTO;
import com.catedra.catalogo.exception.CatalogDiscontinuityException;
import com.catedra.catalogo.repository.CatalogVersionRepository;
import com.catedra.catalogo.repository.CategoryRepository;
import com.catedra.catalogo.repository.ProfessionalRepository;
import com.catedra.catalogo.repository.RedisSyncRepository;
import com.catedra.catalogo.repository.WeeklyScheduleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {

    private static final String CATEGORIES_HASH = "catedra:sync:professional-categories";
    private static final String PROFESSIONALS_HASH = "catedra:sync:professionals";
    private static final String WEEKLY_SCHEDULES_HASH = "catedra:sync:weekly-schedules";

    private final CatedraApiClient catedraApiClient;
    private final CategoryRepository categoryRepository;
    private final ProfessionalRepository professionalRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final CatalogVersionRepository versionRepository;
    private final RedisSyncRepository redisSyncRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final SyncStatusService statusService;

    private final ReentrantLock syncLock = new ReentrantLock();

    @Override
    public CatalogSnapshotDTO syncSnapshot() {
        syncLock.lock();
        try {
            statusService.markSyncing();
            CatalogSnapshotDTO snapshot = doSnapshot();
            statusService.markIdle();
            return snapshot;
        } catch (RuntimeException e) {
            statusService.markError(message(e));
            throw e;
        } finally {
            syncLock.unlock();
        }
    }

    @Override
    public void processIncrementalSync() {
        if (!syncLock.tryLock()) {
            log.info("Ya hay una sincronizacion en curso. Se omite este intento.");
            return;
        }
        try {
            statusService.markSyncing();
            doProcessIncremental();
            statusService.markIdle();
        } catch (RuntimeException e) {
            statusService.markError(message(e));
            throw e;
        } finally {
            syncLock.unlock();
        }
    }

    @Override
    public SyncStatusDTO getStatus() {
        Long localVersion = getLocalVersion();
        Long currentVersion = null;
        Long oldestAvailableVersion = null;
        String lastError = statusService.getLastError();

        try {
            currentVersion = redisSyncRepository.getCurrentVersion();
            oldestAvailableVersion = redisSyncRepository.getOldestAvailableVersion();
        } catch (RuntimeException e) {
            if (lastError == null) {
                lastError = message(e);
            }
        }

        Instant lastSyncedAt = versionRepository.findById(1L)
                .map(CatalogVersion::getLastSyncedAt)
                .orElse(null);

        return new SyncStatusDTO(
                localVersion,
                currentVersion,
                oldestAvailableVersion,
                lastSyncedAt,
                lastError,
                statusService.getState()
        );
    }

    private CatalogSnapshotDTO doSnapshot() {
        CatalogSnapshotDTO snapshot = catedraApiClient.fetchSnapshot();

        if (snapshot == null) {
            throw new IllegalStateException("La catedra devolvio un snapshot vacio");
        }
        if (snapshot.snapshotVersion() == null) {
            throw new IllegalStateException("El snapshot no incluye snapshotVersion");
        }

        transactionTemplate.executeWithoutResult(status -> applySnapshot(snapshot));
        log.info("Snapshot aplicado. Version local: {}", snapshot.snapshotVersion());
        return snapshot;
    }

    private void applySnapshot(CatalogSnapshotDTO snapshot) {
        weeklyScheduleRepository.deleteAllInBatch();
        professionalRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();

        Map<Long, Category> categories = new HashMap<>();
        if (snapshot.professionalCategories() != null) {
            for (CategorySnapshotDTO dto : snapshot.professionalCategories()) {
                Category category = toCategory(dto, new Category());
                categoryRepository.save(category);
                categories.put(category.getId(), category);
            }
        }

        Map<Long, Professional> professionals = new HashMap<>();
        if (snapshot.professionals() != null) {
            for (ProfessionalSnapshotDTO dto : snapshot.professionals()) {
                Professional professional = toProfessional(dto, new Professional(), categories);
                professionalRepository.save(professional);
                professionals.put(professional.getId(), professional);
            }
        }

        if (snapshot.weeklySchedules() != null) {
            for (WeeklyScheduleSnapshotDTO dto : snapshot.weeklySchedules()) {
                WeeklySchedule schedule = toWeeklySchedule(dto, new WeeklySchedule(), professionals);
                weeklyScheduleRepository.save(schedule);
            }
        }

        updateLocalVersion(snapshot.snapshotVersion());
    }

    private void doProcessIncremental() {
        Long redisVersion = redisSyncRepository.getCurrentVersion();
        Long oldestVersion = redisSyncRepository.getOldestAvailableVersion();

        if (redisVersion == null) {
            log.warn("No se encontro current-version en Redis.");
            return;
        }

        Long localVersion = getLocalVersion();
        if (localVersion == null) {
            log.info("No hay version local aplicada. Ejecutando snapshot.");
            doSnapshot();
            return;
        }

        if (localVersion.equals(redisVersion)) {
            log.info("El catalogo ya esta en la ultima version ({}).", localVersion);
            return;
        }

        if (oldestVersion == null || localVersion < oldestVersion) {
            log.warn("Version local {} fuera de la ventana disponible (oldest {}). Ejecutando snapshot.",
                    localVersion, oldestVersion);
            doSnapshot();
            return;
        }

        log.info("Recuperando incrementos desde {} hasta {}", localVersion + 1, redisVersion);

        for (long version = localVersion + 1; version <= redisVersion; version++) {
            String changeJson = redisSyncRepository.getChangeForVersion(version);
            if (changeJson == null) {
                log.warn("No se encontro changes:{}. Ejecutando snapshot.", version);
                doSnapshot();
                return;
            }

            CatalogChangeDTO change = read(changeJson, CatalogChangeDTO.class);
            try {
                applyVersion(version, change);
            } catch (CatalogDiscontinuityException e) {
                log.warn("Discontinuidad aplicando version {}. Ejecutando snapshot: {}", version, e.getMessage());
                doSnapshot();
                return;
            }
        }

        log.info("Sincronizacion incremental finalizada. Version local: {}", redisVersion);
    }

    private void applyVersion(long version, CatalogChangeDTO change) {
        if (change.version() != null && !change.version().equals(version)) {
            throw new CatalogDiscontinuityException(
                    "La version del cambio (" + change.version() + ") no coincide con la clave (" + version + ")");
        }

        CatalogChangeDTO.ChangeLists lists = change.changes();
        List<CategorySnapshotDTO> categories = loadCategories(lists);
        List<ProfessionalSnapshotDTO> professionals = loadProfessionals(lists);
        List<WeeklyScheduleSnapshotDTO> schedules = loadSchedules(lists);

        transactionTemplate.executeWithoutResult(status -> {
            applyCategories(categories);
            Map<Long, Category> categoryMap = loadCategoryMap(categories);
            applyProfessionals(professionals, categoryMap);
            applySchedules(schedules);
            updateLocalVersion(version);
        });
    }

    private List<CategorySnapshotDTO> loadCategories(CatalogChangeDTO.ChangeLists lists) {
        if (lists == null || lists.professionalCategories() == null) {
            return List.of();
        }

        List<CategorySnapshotDTO> result = new ArrayList<>();
        for (Long id : lists.professionalCategories()) {
            String json = redisSyncRepository.getEntityFromHash(CATEGORIES_HASH, id);
            if (json == null) {
                throw new CatalogDiscontinuityException("Falta la categoria " + id + " en Redis");
            }
            result.add(read(json, CategorySnapshotDTO.class));
        }
        return result;
    }

    private List<ProfessionalSnapshotDTO> loadProfessionals(CatalogChangeDTO.ChangeLists lists) {
        if (lists == null || lists.professionals() == null) {
            return List.of();
        }

        List<ProfessionalSnapshotDTO> result = new ArrayList<>();
        for (Long id : lists.professionals()) {
            String json = redisSyncRepository.getEntityFromHash(PROFESSIONALS_HASH, id);
            if (json == null) {
                throw new CatalogDiscontinuityException("Falta el profesional " + id + " en Redis");
            }
            result.add(read(json, ProfessionalSnapshotDTO.class));
        }
        return result;
    }

    private List<WeeklyScheduleSnapshotDTO> loadSchedules(CatalogChangeDTO.ChangeLists lists) {
        if (lists == null || lists.weeklySchedules() == null) {
            return List.of();
        }

        List<WeeklyScheduleSnapshotDTO> result = new ArrayList<>();
        for (Long id : lists.weeklySchedules()) {
            String json = redisSyncRepository.getEntityFromHash(WEEKLY_SCHEDULES_HASH, id);
            if (json == null) {
                throw new CatalogDiscontinuityException("Falta el horario " + id + " en Redis");
            }
            result.add(read(json, WeeklyScheduleSnapshotDTO.class));
        }
        return result;
    }

    private Map<Long, Category> loadCategoryMap(List<CategorySnapshotDTO> categories) {
        Map<Long, Category> result = new HashMap<>();
        for (CategorySnapshotDTO dto : categories) {
            if (dto.id() != null) {
                categoryRepository.findById(dto.id()).ifPresent(category -> result.put(dto.id(), category));
            }
        }
        return result;
    }

    private void applyCategories(List<CategorySnapshotDTO> categories) {
        for (CategorySnapshotDTO dto : categories) {
            Category category = categoryRepository.findById(dto.id()).orElseGet(Category::new);
            categoryRepository.save(toCategory(dto, category));
        }
    }

    private void applyProfessionals(List<ProfessionalSnapshotDTO> professionals, Map<Long, Category> categories) {
        for (ProfessionalSnapshotDTO dto : professionals) {
            Professional professional = professionalRepository.findById(dto.id()).orElseGet(Professional::new);
            professionalRepository.save(toProfessional(dto, professional, categories));
        }
    }

    private void applySchedules(List<WeeklyScheduleSnapshotDTO> schedules) {
        for (WeeklyScheduleSnapshotDTO dto : schedules) {
            WeeklySchedule schedule = weeklyScheduleRepository.findById(dto.id()).orElseGet(WeeklySchedule::new);
            weeklyScheduleRepository.save(toWeeklySchedule(dto, schedule, Map.of()));
        }
    }

    private Category toCategory(CategorySnapshotDTO dto, Category category) {
        category.setId(dto.id());
        category.setName(dto.name());
        category.setDescription(dto.description());
        category.setEnabled(dto.enabled() != null ? dto.enabled() : true);
        category.setCreatedAt(dto.createdAt());
        category.setUpdatedAt(dto.updatedAt());
        return category;
    }

    private Professional toProfessional(ProfessionalSnapshotDTO dto, Professional professional,
                                        Map<Long, Category> categories) {
        professional.setId(dto.id());
        professional.setFirstName(dto.firstName());
        professional.setLastName(dto.lastName());
        professional.setEnabled(dto.enabled() != null ? dto.enabled() : true);
        professional.setCreatedAt(dto.createdAt());
        professional.setUpdatedAt(dto.updatedAt());

        if (dto.categoryId() == null) {
            professional.setCategory(null);
        } else {
            Category category = categories.get(dto.categoryId());
            if (category == null) {
                category = categoryRepository.findById(dto.categoryId())
                        .orElseThrow(() -> new CatalogDiscontinuityException(
                                "No existe la categoria " + dto.categoryId() + " del profesional " + dto.id()));
            }
            professional.setCategory(category);
        }
        return professional;
    }

    private WeeklySchedule toWeeklySchedule(WeeklyScheduleSnapshotDTO dto, WeeklySchedule schedule,
                                            Map<Long, Professional> professionals) {
        schedule.setId(dto.id());
        schedule.setDayOfWeek(parseDayOfWeek(dto.dayOfWeek()));
        schedule.setStartTime(dto.startTime());
        schedule.setEndTime(dto.endTime());
        schedule.setSlotDurationMinutes(dto.slotDurationMinutes());
        schedule.setEnabled(dto.enabled() != null ? dto.enabled() : true);
        schedule.setCreatedAt(dto.createdAt());
        schedule.setUpdatedAt(dto.updatedAt());

        if (dto.professionalId() == null) {
            schedule.setProfessional(null);
        } else {
            Professional professional = professionals.get(dto.professionalId());
            if (professional == null) {
                professional = professionalRepository.findById(dto.professionalId())
                        .orElseThrow(() -> new CatalogDiscontinuityException(
                                "No existe el profesional " + dto.professionalId() + " del horario " + dto.id()));
            }
            schedule.setProfessional(professional);
        }
        return schedule;
    }

    private DayOfWeek parseDayOfWeek(String value) {
        if (value == null) {
            throw new CatalogDiscontinuityException("El horario no incluye dayOfWeek");
        }
        try {
            return DayOfWeek.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new CatalogDiscontinuityException("dayOfWeek invalido: " + value);
        }
    }

    private void updateLocalVersion(Long newVersion) {
        CatalogVersion catalogVersion = versionRepository.findById(1L)
                .orElseGet(() -> new CatalogVersion(newVersion));
        catalogVersion.setCurrentVersion(newVersion);
        catalogVersion.setLastSyncedAt(Instant.now());
        versionRepository.save(catalogVersion);
    }

    private Long getLocalVersion() {
        return versionRepository.findById(1L)
                .map(CatalogVersion::getCurrentVersion)
                .orElse(null);
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new CatalogDiscontinuityException("JSON invalido recibido de Redis: " + e.getOriginalMessage());
        }
    }

    private String message(Throwable throwable) {
        String message = throwable.getMessage();
        return message != null ? message : throwable.getClass().getSimpleName();
    }
}
