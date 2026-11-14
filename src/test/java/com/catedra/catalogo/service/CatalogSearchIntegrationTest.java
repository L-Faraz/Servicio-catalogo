package com.catedra.catalogo.service;

import com.catedra.catalogo.domain.Category;
import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import com.catedra.catalogo.dto.ProfessionalDTO;
import com.catedra.catalogo.repository.CategoryRepository;
import com.catedra.catalogo.repository.ProfessionalRepository;
import com.catedra.catalogo.repository.WeeklyScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogSearchIntegrationTest {

    @Autowired
    private CatalogService catalogService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProfessionalRepository professionalRepository;
    @Autowired
    private WeeklyScheduleRepository weeklyScheduleRepository;

    private Category cardiology;
    private Category dermatology;

    @BeforeEach
    void setUp() {
        weeklyScheduleRepository.deleteAllInBatch();
        professionalRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();

        cardiology = categoryRepository.save(new Category(1L, "Cardiologia", "Corazon", true));
        dermatology = categoryRepository.save(new Category(2L, "Dermatologia", "Piel", false));

        Professional ana = new Professional();
        ana.setId(15L);
        ana.setFirstName("Ana");
        ana.setLastName("Gomez");
        ana.setCategory(cardiology);
        ana.setEnabled(true);
        professionalRepository.save(ana);

        Professional beto = new Professional();
        beto.setId(16L);
        beto.setFirstName("Beto");
        beto.setLastName("Perez");
        beto.setCategory(dermatology);
        beto.setEnabled(false);
        professionalRepository.save(beto);

        weeklyScheduleRepository.save(newSchedule(21L, ana, DayOfWeek.MONDAY, true));
        weeklyScheduleRepository.save(newSchedule(22L, beto, DayOfWeek.TUESDAY, false));
    }

    @Test
    void filtersByCategoryNameEnabledAndAvailability() {
        Page<ProfessionalDTO> result = catalogService.searchProfessionals(
                1L, "ana", true, DayOfWeek.MONDAY, LocalTime.of(10, 0), PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).firstName()).isEqualTo("Ana");
    }

    @Test
    void disabledProfessionalOnlyAppearsWhenRequested() {
        Page<ProfessionalDTO> enabledOnly = catalogService.searchProfessionals(
                null, null, true, null, null, PageRequest.of(0, 20));
        Page<ProfessionalDTO> disabledOnly = catalogService.searchProfessionals(
                null, null, false, null, null, PageRequest.of(0, 20));

        assertThat(enabledOnly.getContent()).extracting(ProfessionalDTO::id).containsExactly(15L);
        assertThat(disabledOnly.getContent()).extracting(ProfessionalDTO::id).containsExactly(16L);
    }

    @Test
    void availabilityRequiresEnabledScheduleInTheRequestedSlot() {
        Page<ProfessionalDTO> wrongDay = catalogService.searchProfessionals(
                null, null, true, DayOfWeek.TUESDAY, null, PageRequest.of(0, 20));
        Page<ProfessionalDTO> outsideHours = catalogService.searchProfessionals(
                null, null, true, DayOfWeek.MONDAY, LocalTime.of(13, 0), PageRequest.of(0, 20));

        assertThat(wrongDay.getContent()).isEmpty();
        assertThat(outsideHours.getContent()).isEmpty();
    }

    private WeeklySchedule newSchedule(Long id, Professional professional, DayOfWeek day, boolean enabled) {
        WeeklySchedule schedule = new WeeklySchedule();
        schedule.setId(id);
        schedule.setProfessional(professional);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(LocalTime.of(9, 0));
        schedule.setEndTime(LocalTime.of(12, 0));
        schedule.setSlotDurationMinutes(30);
        schedule.setEnabled(enabled);
        return schedule;
    }
}
