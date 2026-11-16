package com.catedra.catalogo.repository;

import com.catedra.catalogo.domain.WeeklySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, Long> {
    List<WeeklySchedule> findByProfessionalId(Long professionalId);
    List<WeeklySchedule> findByProfessionalIdAndEnabledTrue(Long professionalId);
}
