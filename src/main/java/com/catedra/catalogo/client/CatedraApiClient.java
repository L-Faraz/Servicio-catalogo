package com.catedra.catalogo.client;

import com.catedra.catalogo.config.CatedraProperties;
import com.catedra.catalogo.dto.CatalogSnapshotDTO;
import com.catedra.catalogo.exception.IntegrationNotConfiguredException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class CatedraApiClient {

    private final RestTemplate restTemplate;
    private final CatedraProperties properties;

    public CatedraApiClient(RestTemplate restTemplate, CatedraProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public CatalogSnapshotDTO fetchSnapshot() {
        if (!StringUtils.hasText(properties.getApiBaseUrl()) || !StringUtils.hasText(properties.getJwtToken())) {
            throw new IntegrationNotConfiguredException("La integracion con la catedra no esta configurada");
        }

        String url = properties.getApiBaseUrl() + "/api/synchronization/snapshot";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getJwtToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<CatalogSnapshotDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                CatalogSnapshotDTO.class
        );

        return response.getBody();
    }
}
