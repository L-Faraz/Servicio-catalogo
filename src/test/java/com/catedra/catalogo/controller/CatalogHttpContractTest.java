package com.catedra.catalogo.controller;

import com.catedra.catalogo.domain.Category;
import com.catedra.catalogo.domain.Professional;
import com.catedra.catalogo.domain.WeeklySchedule;
import com.catedra.catalogo.repository.CategoryRepository;
import com.catedra.catalogo.repository.ProfessionalRepository;
import com.catedra.catalogo.repository.WeeklyScheduleRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CatalogHttpContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProfessionalRepository professionalRepository;
    @Autowired
    private WeeklyScheduleRepository weeklyScheduleRepository;

    @Value("${app.security.jwt.secret}")
    private String secret;
    @Value("${app.security.jwt.issuer}")
    private String issuer;

    @BeforeEach
    void setUp() {
        weeklyScheduleRepository.deleteAllInBatch();
        professionalRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();

        Category cardiology = categoryRepository.save(new Category(3L, "Cardiologia", "Corazon", true));
        categoryRepository.save(new Category(4L, "Dermatologia", "Piel", false));

        Professional ana = new Professional();
        ana.setId(15L);
        ana.setFirstName("Ana");
        ana.setLastName("Gomez");
        ana.setCategory(cardiology);
        ana.setEnabled(true);
        professionalRepository.save(ana);

        WeeklySchedule enabled = new WeeklySchedule();
        enabled.setId(41L);
        enabled.setProfessional(ana);
        enabled.setDayOfWeek(DayOfWeek.MONDAY);
        enabled.setStartTime(LocalTime.of(9, 0));
        enabled.setEndTime(LocalTime.of(17, 0));
        enabled.setSlotDurationMinutes(30);
        enabled.setEnabled(true);
        weeklyScheduleRepository.save(enabled);

        WeeklySchedule disabled = new WeeklySchedule();
        disabled.setId(42L);
        disabled.setProfessional(ana);
        disabled.setDayOfWeek(DayOfWeek.TUESDAY);
        disabled.setStartTime(LocalTime.of(9, 0));
        disabled.setEndTime(LocalTime.of(17, 0));
        disabled.setSlotDurationMinutes(30);
        disabled.setEnabled(false);
        weeklyScheduleRepository.save(disabled);
    }

    @Test
    void categoriesEndpointFiltersByEnabled() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .param("enabled", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].name").value("Cardiologia"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void professionalsEndpointReturnsSpringPageMetadata() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("categoryId", "3")
                        .param("name", "ana")
                        .param("dayOfWeek", "MONDAY")
                        .param("time", "10:00")
                        .param("page", "0")
                        .param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, bearer("SERVICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(15))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void professionalDetailIncludesAllSchedulesAndSchedulesEndpointOnlyEnabled() throws Exception {
        mockMvc.perform(get("/api/professionals/15")
                        .header(HttpHeaders.AUTHORIZATION, bearer("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules.length()").value(2));

        mockMvc.perform(get("/api/professionals/15/schedules")
                        .header(HttpHeaders.AUTHORIZATION, bearer("SERVICE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(41))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void invalidPaginationParameterReturnsProblemDetails() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("categoryId", "0")
                        .header(HttpHeaders.AUTHORIZATION, bearer("USER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void synchronizationAndMetricsRequirePrivilegedRole() throws Exception {
        mockMvc.perform(get("/api/synchronization/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer("SERVICE")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/synchronization/snapshot")
                        .header(HttpHeaders.AUTHORIZATION, bearer("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/metrics")
                        .header(HttpHeaders.AUTHORIZATION, bearer("SERVICE")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics")
                        .header(HttpHeaders.AUTHORIZATION, bearer("USER")))
                .andExpect(status().isForbidden());
    }

    private String bearer(String role) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("test-" + role.toLowerCase())
                .issuedAt(now.minusSeconds(5))
                .expiresAt(now.plusSeconds(3600))
                .claim("roles", List.of(role))
                .build();
        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }
}
