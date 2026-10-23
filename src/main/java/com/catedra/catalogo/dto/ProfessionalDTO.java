package com.catedra.catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private CategoryDTO category;
    private List<WeeklyScheduleDTO> schedules;
    private boolean active;
}