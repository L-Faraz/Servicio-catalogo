package com.catedra.catalogo.client;

import com.catedra.catalogo.config.CatedraProperties;
import com.catedra.catalogo.dto.CatalogSnapshotDTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CatedraApiClient {

    private final RestTemplate restTemplate;
    private final CatedraProperties properties;

    public CatedraApiClient(CatedraProperties properties) {
        this.restTemplate = new RestTemplate();
        this.properties = properties;
    }

    // MÉTODO PRINCIPAL: Va a buscar los datos
    public CatalogSnapshotDTO fetchSnapshot() {
        String url = properties.getApiBaseUrl() + "/api/synchronization/snapshot";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getJwtToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // exchange() hace el viaje web y convierte el JSON en CatalogSnapshotDTO.class
        ResponseEntity<CatalogSnapshotDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                CatalogSnapshotDTO.class
        );

        return response.getBody();
    }
}