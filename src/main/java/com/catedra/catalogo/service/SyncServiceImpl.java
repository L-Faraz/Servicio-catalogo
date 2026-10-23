package com.catedra.catalogo.service;

import com.catedra.catalogo.client.CatedraApiClient;
import com.catedra.catalogo.domain.Category;
import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import com.catedra.catalogo.dto.CatalogSnapshotDTO;
import com.catedra.catalogo.repository.CategoryRepository;
import com.catedra.catalogo.repository.ProfessionalRepository;
import com.catedra.catalogo.repository.WeeklyScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncServiceImpl implements SyncService {

    private final CatedraApiClient catedraApiClient;
    private final CategoryRepository categoryRepository;
    private final ProfessionalRepository professionalRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;

    public SyncServiceImpl(
            CatedraApiClient catedraApiClient,
            CategoryRepository categoryRepository,
            ProfessionalRepository professionalRepository,
            WeeklyScheduleRepository weeklyScheduleRepository) {
        this.catedraApiClient = catedraApiClient;
        this.categoryRepository = categoryRepository;
        this.professionalRepository = professionalRepository;
        this.weeklyScheduleRepository = weeklyScheduleRepository;
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

        return snapshot;
    }
}