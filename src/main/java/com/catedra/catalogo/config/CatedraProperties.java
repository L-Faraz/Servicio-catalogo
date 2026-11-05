package com.catedra.catalogo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "catedra.integration")
public class CatedraProperties {

    private String apiBaseUrl;
    private String jwtToken;
    private String groupId;

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    public String getJwtToken() { return jwtToken; }
    public void setJwtToken(String jwtToken) { this.jwtToken = jwtToken; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    // Propiedades para Kafka
    private String kafkaBootstrapServers;
    private String kafkaGroupId;

    public String getKafkaBootstrapServers() { return kafkaBootstrapServers; }
    public void setKafkaBootstrapServers(String kafkaBootstrapServers) { this.kafkaBootstrapServers = kafkaBootstrapServers; }

    public String getKafkaGroupId() { return kafkaGroupId; }
    public void setKafkaGroupId(String kafkaGroupId) { this.kafkaGroupId = kafkaGroupId; }

}