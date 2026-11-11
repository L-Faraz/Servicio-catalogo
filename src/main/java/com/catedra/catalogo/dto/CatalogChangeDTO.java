package com.catedra.catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogChangeDTO {
    private Long version;
    private ChangeLists changes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeLists {
        private List<Long> professionalCategories;
        private List<Long> professionals;
        private List<Long> weeklySchedules;
    }
}