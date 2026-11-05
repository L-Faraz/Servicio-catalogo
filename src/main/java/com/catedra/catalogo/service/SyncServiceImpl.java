package com.catedra.catalogo.service;

import com.catedra.catalogo.client.CatedraApiClient;
import com.catedra.catalogo.domain.CatalogVersion;
import com.catedra.catalogo.domain.Category;
import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import com.catedra.catalogo.dto.CatalogSnapshotDTO;
import com.catedra.catalogo.repository.CatalogVersionRepository;
import com.catedra.catalogo.repository.CategoryRepository;
import com.catedra.catalogo.repository.ProfessionalRepository;
import com.catedra.catalogo.repository.RedisSyncRepository;
import com.catedra.catalogo.repository.WeeklyScheduleRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SyncServiceImpl implements SyncService {

    private final CatedraApiClient catedraApiClient;
    private final CategoryRepository categoryRepository;
    private final ProfessionalRepository professionalRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final RedisSyncRepository redisSyncRepository;
    private final CatalogVersionRepository versionRepository;
    private final StringRedisTemplate redisTemplate;

    public SyncServiceImpl(
            CatedraApiClient catedraApiClient,
            CategoryRepository categoryRepository,
            ProfessionalRepository professionalRepository,
            WeeklyScheduleRepository weeklyScheduleRepository,
            RedisSyncRepository redisSyncRepository,
            CatalogVersionRepository versionRepository,
            StringRedisTemplate redisTemplate) {
        this.catedraApiClient = catedraApiClient;
        this.categoryRepository = categoryRepository;
        this.professionalRepository = professionalRepository;
        this.weeklyScheduleRepository = weeklyScheduleRepository;
        this.redisSyncRepository = redisSyncRepository;
        this.versionRepository = versionRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public CatalogSnapshotDTO syncSnapshot() {
        CatalogSnapshotDTO snapshot = catedraApiClient.fetchSnapshot();

        if (snapshot == null) {
            return null;
        }

        if (snapshot.categories() != null) {
            snapshot.categories().forEach(dto -> {
                Category category = categoryRepository.findById(dto.id())
                        .orElseGet(Category::new);
                category.setId(dto.id());
                category.setName(dto.name());
                category.setEnabled(dto.enabled());
                categoryRepository.save(category);
            });
        }

        if (snapshot.professionals() != null) {
            snapshot.professionals().forEach(dto -> {
                Professional professional = professionalRepository.findById(dto.id())
                        .orElseGet(Professional::new);
                professional.setId(dto.id());
                professional.setName(dto.name());
                professional.setLastName(dto.lastName());
                professional.setEmail(dto.email());
                professional.setEnabled(dto.enabled());

                if (dto.categoryId() != null) {
                    categoryRepository.findById(dto.categoryId())
                            .ifPresent(professional::setCategory);
                }

                professionalRepository.save(professional);
            });
        }

        if (snapshot.weeklySchedules() != null) {
            snapshot.weeklySchedules().forEach(dto -> {
                WeeklySchedule schedule = weeklyScheduleRepository.findById(dto.id())
                        .orElseGet(WeeklySchedule::new);
                schedule.setId(dto.id());
                schedule.setDayOfWeek(dto.dayOfWeek());
                schedule.setStartTime(dto.startTime());
                schedule.setEndTime(dto.endTime());
                schedule.setSlotDurationMinutes(dto.slotDurationMinutes());
                schedule.setEnabled(dto.enabled());

                if (dto.professionalId() != null) {
                    professionalRepository.findById(dto.professionalId())
                            .ifPresent(schedule::setProfessional);
                }

                weeklyScheduleRepository.save(schedule);
            });
        }

        // Si Redis nos da la versión actual tras el Snapshot, la dejamos registrada
        Long redisVersion = redisSyncRepository.getCurrentVersion();
        if (redisVersion != null) {
            CatalogVersion catalogVersion = versionRepository.findById(1L)
                    .orElse(new CatalogVersion(redisVersion));
            catalogVersion.setCurrentVersion(redisVersion);
            catalogVersion.setLastSyncedAt(LocalDateTime.now());
            versionRepository.save(catalogVersion);
        }

        return snapshot;
    }

    @Override
    @Transactional
    public void processIncrementalSync() {
        Long redisVersion = redisSyncRepository.getCurrentVersion();
        Long oldestVersion = redisSyncRepository.getOldestAvailableVersion();

        if (redisVersion == null) {
            return;
        }

        Long localVersion = versionRepository.findById(1L)
                .map(CatalogVersion::getCurrentVersion)
                .orElse(0L);

        if (localVersion.equals(redisVersion)) {
            return;
        }

        // Fallback: Si estamos más atrasados que la versión más vieja disponible en Redis,
        // forzamos un Snapshot completo
        if (oldestVersion != null && localVersion < oldestVersion) {
            syncSnapshot();
            return;
        }

        // Procesar cambios incrementales
        for (long v = localVersion + 1; v <= redisVersion; v++) {
            String changeJson = redisTemplate.opsForValue().get("catedra:sync:changes:" + v);
            if (changeJson != null) {
                applyChange(changeJson);
            }
        }

        CatalogVersion currentCatalogVersion = versionRepository.findById(1L)
                .orElse(new CatalogVersion(redisVersion));
        currentCatalogVersion.setCurrentVersion(redisVersion);
        currentCatalogVersion.setLastSyncedAt(LocalDateTime.now());
        versionRepository.save(currentCatalogVersion);
    }

    private void applyChange(String changeJson) {
        // TODO: Deserializar el JSON y aplicar las mutaciones correspondientes
    }
}