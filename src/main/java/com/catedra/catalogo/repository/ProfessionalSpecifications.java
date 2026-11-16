package com.catedra.catalogo.repository;

import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class ProfessionalSpecifications {

    private ProfessionalSpecifications() {
    }

    public static Specification<Professional> hasCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null
                ? cb.conjunction()
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Professional> hasEnabled(Boolean enabled) {
        return (root, query, cb) -> enabled == null
                ? cb.conjunction()
                : cb.equal(root.get("enabled"), enabled);
    }

    public static Specification<Professional> nameContains(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + name.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern)
            );
        };
    }

    public static Specification<Professional> hasAvailability(DayOfWeek dayOfWeek, LocalTime time) {
        return (root, query, cb) -> {
            if (dayOfWeek == null && time == null) {
                return cb.conjunction();
            }

            Join<Professional, WeeklySchedule> schedule = root.join("schedules", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(schedule.get("enabled")));

            if (dayOfWeek != null) {
                predicates.add(cb.equal(schedule.get("dayOfWeek"), dayOfWeek));
            }
            if (time != null) {
                predicates.add(cb.lessThanOrEqualTo(schedule.get("startTime"), time));
                predicates.add(cb.greaterThan(schedule.get("endTime"), time));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
