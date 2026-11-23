package com.catedra.catalogo.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class CatedraIntegrationEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(CatedraIntegrationEnvironmentPostProcessor.class);
    private static final String PROPERTY_SOURCE_NAME = "catedraIntegration";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String apiBaseUrl = trimToNull(environment.getProperty("CATEDRA_API_URL"));
        String username = trimToNull(environment.getProperty("CATEDRA_LOGIN_USERNAME"));
        String password = trimToNull(environment.getProperty("CATEDRA_LOGIN_PASSWORD"));
        if (apiBaseUrl == null || username == null || password == null) {
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            String baseUrl = apiBaseUrl.replaceAll("/+$", "");

            HttpRequest loginRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/api/authenticate"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of(
                            "username", username,
                            "password", password,
                            "rememberMe", true
                    ))))
                    .build();
            HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
            requireSuccess(loginResponse, "login técnico de cátedra");
            String token = text(mapper.readTree(loginResponse.body()), "id_token");
            if (!StringUtils.hasText(token)) {
                throw new IllegalStateException("La respuesta de login técnico no incluyó id_token");
            }

            HttpRequest integrationRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/api/student/integration"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> integrationResponse = client.send(integrationRequest, HttpResponse.BodyHandlers.ofString());
            requireSuccess(integrationResponse, "recuperación de integración de cátedra");
            JsonNode integration = mapper.readTree(integrationResponse.body());

            Map<String, Object> properties = new HashMap<>();
            properties.put("catedra.integration.jwt-token", token);
            putIfPresent(properties, "catedra.integration.group-id", integration, "groupId");
            putIfPresent(properties, "catedra.integration.redis-host", integration, "redisHost");
            putIfPresent(properties, "catedra.integration.redis-port", integration, "redisPort");
            putIfPresent(properties, "catedra.integration.redis-username", integration, "redisUsername");
            putIfPresent(properties, "catedra.integration.redis-password", integration, "redisPassword");
            putIfPresent(properties, "catedra.integration.redis-read-namespace", integration, "redisReadNamespace");
            putIfPresent(properties, "catedra.integration.redis-write-namespace", integration, "redisWriteNamespace");
            putIfPresent(properties, "catedra.integration.kafka-bootstrap-servers", integration, "kafkaBootstrapServers");
            putIfPresent(properties, "catedra.integration.kafka-consumer-group-id", integration, "kafkaConsumerGroupId");
            putIfPresent(properties, "catedra.integration.kafka-catalog-topic", integration, "kafkaCatalogTopic");
            putIfPresent(properties, "catedra.integration.kafka-dlt-topic", integration, "kafkaDltTopic");
            putIfPresent(properties, "catedra.integration.provisioning-status", integration, "provisioningStatus");
            if (!StringUtils.hasText((CharSequence) properties.get("catedra.integration.group-id"))) {
                throw new IllegalStateException("La integración de cátedra no incluyó groupId");
            }

            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
            log.info("Configuración técnica de cátedra cargada para el grupo {}", properties.get("catedra.integration.group-id"));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió la carga de configuración técnica de cátedra", exception);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("No se pudo cargar la configuración técnica de cátedra", exception);
        }
    }

    private static void requireSuccess(HttpResponse<String> response, String operation) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(operation + " respondió HTTP " + response.statusCode());
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static void putIfPresent(Map<String, Object> properties, String key, JsonNode node, String field) {
        String value = text(node, field);
        if (StringUtils.hasText(value)) {
            properties.put(key, value);
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
