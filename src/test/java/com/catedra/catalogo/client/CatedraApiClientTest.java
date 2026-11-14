package com.catedra.catalogo.client;

import com.catedra.catalogo.config.CatedraProperties;
import com.catedra.catalogo.config.RestClientConfig;
import com.catedra.catalogo.dto.CatalogSnapshotDTO;
import com.catedra.catalogo.exception.IntegrationNotConfiguredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatedraApiClientTest {

    private static final String SNAPSHOT_JSON = """
            {"snapshotVersion":7,"generatedAt":"2026-09-10T13:40:00Z",
             "professionalCategories":[
               {"id":1,"name":"Clinica medica","description":"Atencion clinica general","enabled":true,
                "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}],
             "professionals":[
               {"id":15,"categoryId":1,"firstName":"Ana","lastName":"Gomez","enabled":true,
                "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}],
             "weeklySchedules":[
               {"id":21,"professionalId":15,"dayOfWeek":"MONDAY","startTime":"09:00:00",
                "endTime":"12:00:00","slotDurationMinutes":30,"enabled":true,
                "createdAt":"2026-07-01T12:00:00Z","updatedAt":"2026-09-10T13:40:00Z"}]}
            """;

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private CatedraProperties properties;
    private CatedraApiClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestClientConfig().restTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        properties = new CatedraProperties();
        properties.setApiBaseUrl("http://catedra.test");
        properties.setJwtToken("technical-token");
        client = new CatedraApiClient(restTemplate, properties);
    }

    @Test
    void fetchSnapshotUsesBearerTokenAndParsesContract() {
        server.expect(requestTo("http://catedra.test/api/synchronization/snapshot"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer technical-token"))
                .andRespond(withSuccess(SNAPSHOT_JSON, MediaType.APPLICATION_JSON));

        CatalogSnapshotDTO snapshot = client.fetchSnapshot();

        assertThat(snapshot.snapshotVersion()).isEqualTo(7L);
        assertThat(snapshot.professionalCategories()).hasSize(1);
        assertThat(snapshot.professionals().get(0).firstName()).isEqualTo("Ana");
        assertThat(snapshot.weeklySchedules().get(0).dayOfWeek()).isEqualTo("MONDAY");
        server.verify();
    }

    @Test
    void missingConfigurationFailsFast() {
        properties.setJwtToken(" ");

        assertThatThrownBy(() -> client.fetchSnapshot())
                .isInstanceOf(IntegrationNotConfiguredException.class);
    }
}
