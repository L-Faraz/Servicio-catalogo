package com.catedra.catalogo.service;

import com.catedra.catalogo.client.CatedraApiClient;
import com.catedra.catalogo.domain.CatalogVersion;
import com.catedra.catalogo.domain.Category;
import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import com.catedra.catalogo.dto.CatalogSnapshotDTO;
import com.catedra.catalogo.dto.CategorySnapshotDTO;
import com.catedra.catalogo.dto.ProfessionalSnapshotDTO;
import com.catedra.catalogo.dto.WeeklyScheduleSnapshotDTO;
import com.catedra.catalogo.repository.CatalogVersionRepository;
import com.catedra.catalogo.repository.CategoryRepository;
import com.catedra.catalogo.repository.ProfessionalRepository;
import com.catedra.catalogo.repository.RedisSyncRepository;
import com.catedra.catalogo.repository.WeeklyScheduleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-10T13:40:00Z");

    @Mock
    private CatedraApiClient catedraApiClient;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProfessionalRepository professionalRepository;
    @Mock
    private WeeklyScheduleRepository weeklyScheduleRepository;
    @Mock
    private CatalogVersionRepository versionRepository;
    @Mock
    private RedisSyncRepository redisSyncRepository;
    @Mock
    private SyncStatusService statusService;
    @Mock
    private TransactionTemplate transactionTemplate;

    private SyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        syncService = new SyncServiceImpl(
                catedraApiClient,
                categoryRepository,
                professionalRepository,
                weeklyScheduleRepository,
                versionRepository,
                redisSyncRepository,
                objectMapper,
                transactionTemplate,
                statusService
        );
    }

    @Test
    void snapshotPersistsFullCatalogAndVersion() {
        CatalogSnapshotDTO snapshot = snapshot(7L);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot);
        when(versionRepository.findById(1L)).thenReturn(Optional.empty());
        doAnswer(invocation -> invocation.getArgument(0)).when(categoryRepository).save(any());
        doAnswer(invocation -> invocation.getArgument(0)).when(professionalRepository).save(any());

        syncService.syncSnapshot();

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getDescription()).isEqualTo("Atencion clinica general");
        assertThat(categoryCaptor.getValue().getEnabled()).isTrue();

        ArgumentCaptor<Professional> professionalCaptor = ArgumentCaptor.forClass(Professional.class);
        verify(professionalRepository).save(professionalCaptor.capture());
        assertThat(professionalCaptor.getValue().getFirstName()).isEqualTo("Ana");
        assertThat(professionalCaptor.getValue().getLastName()).isEqualTo("Gomez");

        ArgumentCaptor<WeeklySchedule> scheduleCaptor = ArgumentCaptor.forClass(WeeklySchedule.class);
        verify(weeklyScheduleRepository).save(scheduleCaptor.capture());
        assertThat(scheduleCaptor.getValue().getDayOfWeek().name()).isEqualTo("MONDAY");
        assertThat(scheduleCaptor.getValue().getSlotDurationMinutes()).isEqualTo(30);

        verify(versionRepository).save(any(CatalogVersion.class));
        verify(statusService).markIdle();
    }

    @Test
    void snapshotWithoutVersionFailsAndDoesNotPersist() {
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot(null));

        assertThatThrownBy(() -> syncService.syncSnapshot())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("snapshotVersion");

        verify(categoryRepository, never()).save(any());
        verify(statusService).markError(anyString());
    }

    @Test
    void oldLocalVersionTriggersSnapshot() {
        when(redisSyncRepository.getCurrentVersion()).thenReturn(7L);
        when(redisSyncRepository.getOldestAvailableVersion()).thenReturn(4L);
        when(versionRepository.findById(1L)).thenReturn(Optional.of(new CatalogVersion(3L)));
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot(7L));
        doAnswer(invocation -> invocation.getArgument(0)).when(categoryRepository).save(any());
        doAnswer(invocation -> invocation.getArgument(0)).when(professionalRepository).save(any());

        syncService.processIncrementalSync();

        verify(catedraApiClient).fetchSnapshot();
        verify(redisSyncRepository, never()).getChangeForVersion(anyLong());
        verify(statusService).markIdle();
    }

    @Test
    void missingChangeTriggersSnapshot() {
        when(redisSyncRepository.getCurrentVersion()).thenReturn(2L);
        when(redisSyncRepository.getOldestAvailableVersion()).thenReturn(1L);
        when(versionRepository.findById(1L)).thenReturn(Optional.of(new CatalogVersion(1L)));
        when(redisSyncRepository.getChangeForVersion(2L)).thenReturn(null);
        when(catedraApiClient.fetchSnapshot()).thenReturn(snapshot(2L));
        doAnswer(invocation -> invocation.getArgument(0)).when(categoryRepository).save(any());
        doAnswer(invocation -> invocation.getArgument(0)).when(professionalRepository).save(any());

        syncService.processIncrementalSync();

        verify(catedraApiClient).fetchSnapshot();
    }

    @Test
    void incrementalAppliesChangedEntitiesAndAdvancesVersion() {
        Map<Long, Category> savedCategories = new HashMap<>();
        Map<Long, Professional> savedProfessionals = new HashMap<>();

        when(redisSyncRepository.getCurrentVersion()).thenReturn(2L);
        when(redisSyncRepository.getOldestAvailableVersion()).thenReturn(1L);
        when(versionRepository.findById(1L)).thenReturn(Optional.of(new CatalogVersion(1L)));
        when(redisSyncRepository.getChangeForVersion(2L)).thenReturn("""
                {"version":2,"changes":{"professionalCategories":[1],"professionals":[15],"weeklySchedules":[21]}}
                """);
        when(redisSyncRepository.getEntityFromHash("catedra:sync:professional-categories", 1L))
                .thenReturn("""
                        {"id":1,"name":"Clinica medica","description":"Atencion clinica general","enabled":true,
                         "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}
                        """);
        when(redisSyncRepository.getEntityFromHash("catedra:sync:professionals", 15L))
                .thenReturn("""
                        {"id":15,"categoryId":1,"firstName":"Ana","lastName":"Gomez","enabled":true,
                         "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}
                        """);
        when(redisSyncRepository.getEntityFromHash("catedra:sync:weekly-schedules", 21L))
                .thenReturn("""
                        {"id":21,"professionalId":15,"dayOfWeek":"MONDAY","startTime":"09:00:00",
                         "endTime":"12:00:00","slotDurationMinutes":30,"enabled":true,
                         "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}
                        """);

        when(categoryRepository.findById(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(savedCategories.get(invocation.getArgument(0))));
        when(professionalRepository.findById(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(savedProfessionals.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            savedCategories.put(category.getId(), category);
            return category;
        }).when(categoryRepository).save(any());
        doAnswer(invocation -> {
            Professional professional = invocation.getArgument(0);
            savedProfessionals.put(professional.getId(), professional);
            return professional;
        }).when(professionalRepository).save(any());

        syncService.processIncrementalSync();

        assertThat(savedCategories.get(1L).getName()).isEqualTo("Clinica medica");
        assertThat(savedProfessionals.get(15L).getFirstName()).isEqualTo("Ana");
        verify(weeklyScheduleRepository).save(any(WeeklySchedule.class));
        verify(versionRepository).save(any(CatalogVersion.class));
        verify(catedraApiClient, never()).fetchSnapshot();
    }

    @Test
    void alreadyCurrentVersionDoesNothing() {
        when(redisSyncRepository.getCurrentVersion()).thenReturn(7L);
        when(redisSyncRepository.getOldestAvailableVersion()).thenReturn(4L);
        when(versionRepository.findById(1L)).thenReturn(Optional.of(new CatalogVersion(7L)));

        syncService.processIncrementalSync();

        verify(catedraApiClient, never()).fetchSnapshot();
        verify(redisSyncRepository, never()).getChangeForVersion(anyLong());
        verify(statusService).markIdle();
    }

    private CatalogSnapshotDTO snapshot(Long version) {
        return new CatalogSnapshotDTO(
                version,
                NOW,
                List.of(new CategorySnapshotDTO(1L, "Clinica medica", "Atencion clinica general", true, NOW, NOW)),
                List.of(new ProfessionalSnapshotDTO(15L, 1L, "Ana", "Gomez", true, NOW, NOW)),
                List.of(new WeeklyScheduleSnapshotDTO(
                        21L, 15L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(12, 0), 30, true, NOW, NOW))
        );
    }
}
