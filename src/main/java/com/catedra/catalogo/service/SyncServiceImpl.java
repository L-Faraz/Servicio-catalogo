package com.catedra.catalogo.service;

import com.catedra.catalogo.client.CatedraApiClient;
import com.catedra.catalogo.domain.CatalogVersion;
import com.catedra.catalogo.domain.Category;
import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import com.catedra.catalogo.dto.CatalogChangeDTO;
import com.catedra.catalogo.dto.CatalogSnapshotDTO;
import com.catedra.catalogo.dto.CategoryDTO;
import com.catedra.catalogo.dto.ProfessionalDTO;
import com.catedra.catalogo.dto.WeeklyScheduleDTO;
import com.catedra.catalogo.repository.CatalogVersionRepository;
import com.catedra.catalogo.repository.CategoryRepository;
import com.catedra.catalogo.repository.ProfessionalRepository;
import com.catedra.catalogo.repository.RedisSyncRepository;
import com.catedra.catalogo.repository.WeeklyScheduleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {

    private final CatedraApiClient catedraApiClient;
    private final CategoryRepository categoryRepository;
    private final ProfessionalRepository professionalRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final CatalogVersionRepository versionRepository;
    private final RedisSyncRepository redisSyncRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CatalogSnapshotDTO syncSnapshot() {
        log.info("Iniciando sincronización por Snapshot...");

        CatalogSnapshotDTO snapshot = catedraApiClient.fetchSnapshot();

        if (snapshot == null) {
            log.error("El snapshot recibido es nulo. Abortando sincronización.");
            return null;
        }

        weeklyScheduleRepository.deleteAllInBatch();
        professionalRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();

        // 1. Persistir Categorías
        if (snapshot.categories() != null) {
            snapshot.categories().forEach(dto -> {
                Category domainEntity = new Category();
                domainEntity.setId(dto.id());
                domainEntity.setName(dto.name());
                domainEntity.setEnabled(true);
                categoryRepository.save(domainEntity);
            });
        }

        // 2. Persistir Profesionales
        if (snapshot.professionals() != null) {
            snapshot.professionals().forEach(dto -> {
                Professional domainEntity = new Professional();
                domainEntity.setId(dto.id());
                domainEntity.setName(dto.name());
                domainEntity.setLastName(dto.lastName());
                domainEntity.setEmail(dto.email());
                domainEntity.setEnabled(dto.enabled());

                if (dto.categoryId() != null) {
                    categoryRepository.findById(dto.categoryId()).ifPresent(domainEntity::setCategory);
                }
                professionalRepository.save(domainEntity);
            });
        }

        // 3. Persistir Horarios
        if (snapshot.weeklySchedules() != null) {
            snapshot.weeklySchedules().forEach(dto -> {
                WeeklySchedule domainEntity = new WeeklySchedule();
                domainEntity.setId(dto.id());
                if (dto.dayOfWeek() != null) {
                    domainEntity.setDayOfWeek(dto.dayOfWeek());
                }
                domainEntity.setStartTime(dto.startTime());
                domainEntity.setEndTime(dto.endTime());
                domainEntity.setSlotDurationMinutes(dto.slotDurationMinutes());
                if (dto.professionalId() != null) {
                    professionalRepository.findById(dto.professionalId()).ifPresent(domainEntity::setProfessional);
                }
                domainEntity.setEnabled(dto.enabled() != null ? dto.enabled() : true);

                weeklyScheduleRepository.save(domainEntity);
            });
        }

        updateLocalVersion(snapshot.snapshotVersion());
        log.info("Snapshot aplicado exitosamente. Versión actual: {}", snapshot.snapshotVersion());

        return snapshot;
    }

    @Override
    public synchronized void processIncrementalSync() {
        Long redisVersion = redisSyncRepository.getCurrentVersion();
        Long oldestVersion = redisSyncRepository.getOldestAvailableVersion();

        if (redisVersion == null) {
            log.warn("No se encontró versión actual en Redis.");
            return;
        }

        Long localVersion = versionRepository.findById(1L)
                .map(CatalogVersion::getCurrentVersion)
                .orElse(0L);

        if (localVersion.equals(redisVersion)) {
            log.info("El catálogo ya está en la última versión ({}).", localVersion);
            return;
        }

        if (oldestVersion != null && localVersion < (oldestVersion - 1)) {
            log.warn("Versión local ({}) desactualizada (Oldest en Redis: {}). Ejecutando Snapshot...", localVersion, oldestVersion);
            syncSnapshot();
            return;
        }

        log.info("Recuperando incrementos desde versión {} hasta {}", localVersion + 1, redisVersion);

        for (long v = localVersion + 1; v <= redisVersion; v++) {
            String changeJson = redisSyncRepository.getChangeForVersion(v);
            if (changeJson != null) {
                applyVersionChanges(v, changeJson);
            } else {
                log.warn("No se encontró el JSON de cambios para la versión {}. Abortando sincronización incremental.", v);
                syncSnapshot();
                return;
            }
        }

        log.info("Sincronización incremental finalizada. Versión actual: {}", redisVersion);
    }

    @Transactional
    protected void applyVersionChanges(Long versionNumber, String changeJson) {
        try {
            CatalogChangeDTO changeDto = objectMapper.readValue(changeJson, CatalogChangeDTO.class);
            CatalogChangeDTO.ChangeLists lists = changeDto.getChanges();

            if (lists != null) {
                if (lists.getProfessionalCategories() != null) {
                    for (Long id : lists.getProfessionalCategories()) {
                        String json = redisSyncRepository.getEntityFromHash("catedra:sync:professional-categories", id);
                        if (json != null) {
                            CategoryDTO dto = objectMapper.readValue(json, CategoryDTO.class);
                            Category domainEntity = categoryRepository.findById(dto.getId()).orElseGet(Category::new);
                            domainEntity.setId(dto.getId());
                            domainEntity.setName(dto.getName());
                            domainEntity.setEnabled(true);
                            categoryRepository.save(domainEntity);
                        }
                    }
                }

                if (lists.getProfessionals() != null) {
                    for (Long id : lists.getProfessionals()) {
                        String json = redisSyncRepository.getEntityFromHash("catedra:sync:professionals", id);
                        if (json != null) {
                            ProfessionalDTO dto = objectMapper.readValue(json, ProfessionalDTO.class);
                            Professional domainEntity = professionalRepository.findById(dto.getId()).orElseGet(Professional::new);
                            domainEntity.setId(dto.getId());
                            domainEntity.setName(dto.getFirstName());
                            domainEntity.setLastName(dto.getLastName());
                            domainEntity.setEmail(dto.getEmail());
                            domainEntity.setEnabled(dto.isActive());

                            if (dto.getCategory() != null && dto.getCategory().getId() != null) {
                                categoryRepository.findById(dto.getCategory().getId()).ifPresent(domainEntity::setCategory);
                            }
                            professionalRepository.save(domainEntity);
                        }
                    }
                }

                if (lists.getWeeklySchedules() != null) {
                    for (Long id : lists.getWeeklySchedules()) {
                        String json = redisSyncRepository.getEntityFromHash("catedra:sync:weekly-schedules", id);
                        if (json != null) {
                            WeeklyScheduleDTO dto = objectMapper.readValue(json, WeeklyScheduleDTO.class);
                            WeeklySchedule domainEntity = weeklyScheduleRepository.findById(dto.getId()).orElseGet(WeeklySchedule::new);
                            domainEntity.setId(dto.getId());

                            if (dto.getDayOfWeek() != null) {
                                domainEntity.setDayOfWeek(dto.getDayOfWeek().getValue());
                            }
                            domainEntity.setStartTime(dto.getStartTime());
                            domainEntity.setEndTime(dto.getEndTime());
                            domainEntity.setEnabled(true);

                            weeklyScheduleRepository.save(domainEntity);
                        }
                    }
                }
            }

            updateLocalVersion(versionNumber);

        } catch (Exception e) {
            log.error("Error aplicando versión {}: {}", versionNumber, e.getMessage(), e);
            throw new RuntimeException("Fallo al aplicar cambios de la versión " + versionNumber, e);
        }
    }

    @Transactional
    protected void updateLocalVersion(Long newVersion) {
        CatalogVersion catalogVersion = versionRepository.findById(1L).orElseGet(() -> new CatalogVersion(newVersion));
        catalogVersion.setCurrentVersion(newVersion);
        versionRepository.save(catalogVersion);
    }
}