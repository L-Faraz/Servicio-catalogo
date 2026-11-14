package com.catedra.catalogo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "catedra.integration")
public class CatedraProperties {

    private String apiBaseUrl;
    private String jwtToken;
    private String groupId;
    private String redisHost;
    private int redisPort = 6379;
    private String redisUsername;
    private String redisPassword;
    private String redisReadNamespace;
    private String redisWriteNamespace;
    private String kafkaBootstrapServers;
    private String kafkaConsumerGroupId;
    private String kafkaCatalogTopic;
    private boolean kafkaEnabled = true;
    private boolean reconciliationEnabled = true;
    private long reconciliationIntervalMs = 60000;
    private long reconciliationInitialDelayMs = 30000;
    private String provisioningStatus;

    public boolean isConfigured() {
        return StringUtils.hasText(apiBaseUrl)
                && StringUtils.hasText(jwtToken)
                && StringUtils.hasText(redisHost);
    }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    public String getJwtToken() { return jwtToken; }
    public void setJwtToken(String jwtToken) { this.jwtToken = jwtToken; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getRedisHost() { return redisHost; }
    public void setRedisHost(String redisHost) { this.redisHost = redisHost; }

    public int getRedisPort() { return redisPort; }
    public void setRedisPort(int redisPort) { this.redisPort = redisPort; }

    public String getRedisUsername() { return redisUsername; }
    public void setRedisUsername(String redisUsername) { this.redisUsername = redisUsername; }

    public String getRedisPassword() { return redisPassword; }
    public void setRedisPassword(String redisPassword) { this.redisPassword = redisPassword; }

    public String getRedisReadNamespace() { return redisReadNamespace; }
    public void setRedisReadNamespace(String redisReadNamespace) { this.redisReadNamespace = redisReadNamespace; }

    public String getRedisWriteNamespace() { return redisWriteNamespace; }
    public void setRedisWriteNamespace(String redisWriteNamespace) { this.redisWriteNamespace = redisWriteNamespace; }

    public String getKafkaBootstrapServers() { return kafkaBootstrapServers; }
    public void setKafkaBootstrapServers(String kafkaBootstrapServers) { this.kafkaBootstrapServers = kafkaBootstrapServers; }

    public String getKafkaConsumerGroupId() { return kafkaConsumerGroupId; }
    public void setKafkaConsumerGroupId(String kafkaConsumerGroupId) { this.kafkaConsumerGroupId = kafkaConsumerGroupId; }

    public String getKafkaCatalogTopic() { return kafkaCatalogTopic; }
    public void setKafkaCatalogTopic(String kafkaCatalogTopic) { this.kafkaCatalogTopic = kafkaCatalogTopic; }

    public boolean isKafkaEnabled() { return kafkaEnabled; }
    public void setKafkaEnabled(boolean kafkaEnabled) { this.kafkaEnabled = kafkaEnabled; }

    public boolean isReconciliationEnabled() { return reconciliationEnabled; }
    public void setReconciliationEnabled(boolean reconciliationEnabled) { this.reconciliationEnabled = reconciliationEnabled; }

    public long getReconciliationIntervalMs() { return reconciliationIntervalMs; }
    public void setReconciliationIntervalMs(long reconciliationIntervalMs) { this.reconciliationIntervalMs = reconciliationIntervalMs; }

    public long getReconciliationInitialDelayMs() { return reconciliationInitialDelayMs; }
    public void setReconciliationInitialDelayMs(long reconciliationInitialDelayMs) { this.reconciliationInitialDelayMs = reconciliationInitialDelayMs; }

    public String getProvisioningStatus() { return provisioningStatus; }
    public void setProvisioningStatus(String provisioningStatus) { this.provisioningStatus = provisioningStatus; }
}
