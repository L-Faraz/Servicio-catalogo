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
    private String kafkaDltTopic;
    private String kafkaSecurityProtocol = "PLAINTEXT";
    private String kafkaSaslMechanism;
    private String kafkaSaslJaasConfig;
    private String kafkaSslTruststoreLocation;
    private String kafkaSslTruststorePassword;
    private String kafkaSslTruststoreType = "JKS";
    private String kafkaSslKeystoreLocation;
    private String kafkaSslKeystorePassword;
    private String kafkaSslKeystoreType = "JKS";
    private String kafkaSslKeyPassword;
    private String kafkaSslEndpointIdentificationAlgorithm;
    private long kafkaRetryInitialMs = 1000;
    private long kafkaMaxRetries = 3;
    private boolean kafkaEnabled = true;
    private boolean reconciliationEnabled = true;
    private long reconciliationIntervalMs = 60000;
    private long reconciliationInitialDelayMs = 30000;
    private long processedEventsRetentionDays = 7;
    private boolean processedEventsCleanupEnabled = true;
    private long processedEventsCleanupIntervalMs = 3600000;
    private long processedEventsCleanupInitialDelayMs = 60000;
    private String provisioningStatus;

    public boolean isConfigured() {
        return isSnapshotConfigured() && StringUtils.hasText(redisHost);
    }

    public boolean isSnapshotConfigured() {
        return StringUtils.hasText(apiBaseUrl) && StringUtils.hasText(jwtToken);
    }

    public String getEffectiveKafkaCatalogTopic() {
        if (StringUtils.hasText(kafkaCatalogTopic)) {
            return kafkaCatalogTopic;
        }
        return StringUtils.hasText(groupId) ? "catedra.catalog." + groupId : null;
    }

    public String getEffectiveKafkaConsumerGroupId() {
        if (StringUtils.hasText(kafkaConsumerGroupId)) {
            return kafkaConsumerGroupId;
        }
        return StringUtils.hasText(groupId) ? "alumnos-" + groupId : "servicio-catalogo-group";
    }

    public String getEffectiveKafkaDltTopic() {
        if (StringUtils.hasText(kafkaDltTopic)) {
            return kafkaDltTopic;
        }
        String catalogTopic = getEffectiveKafkaCatalogTopic();
        return StringUtils.hasText(catalogTopic) ? catalogTopic + ".dlt" : "catedra.catalog.dlt";
    }

    public String getEffectiveRedisReadNamespace() {
        String namespace = StringUtils.hasText(redisReadNamespace)
                ? redisReadNamespace.trim()
                : "catedra:sync:*";
        if (namespace.endsWith(":*")) {
            return namespace.substring(0, namespace.length() - 1);
        }
        if (namespace.endsWith("*")) {
            return namespace.substring(0, namespace.length() - 1);
        }
        return namespace.endsWith(":") ? namespace : namespace + ":";
    }

    public String getEffectiveRedisWriteNamespace() {
        if (StringUtils.hasText(redisWriteNamespace)) {
            return redisWriteNamespace;
        }
        return StringUtils.hasText(groupId) ? "alumnos:" + groupId + ":*" : null;
    }

    public boolean isProvisioned() {
        return "PROVISIONED".equalsIgnoreCase(provisioningStatus);
    }

    public boolean isProvisioningBlocked() {
        return StringUtils.hasText(provisioningStatus) && !isProvisioned();
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

    public String getKafkaDltTopic() { return kafkaDltTopic; }
    public void setKafkaDltTopic(String kafkaDltTopic) { this.kafkaDltTopic = kafkaDltTopic; }

    public String getKafkaSecurityProtocol() { return kafkaSecurityProtocol; }
    public void setKafkaSecurityProtocol(String kafkaSecurityProtocol) { this.kafkaSecurityProtocol = kafkaSecurityProtocol; }

    public String getKafkaSaslMechanism() { return kafkaSaslMechanism; }
    public void setKafkaSaslMechanism(String kafkaSaslMechanism) { this.kafkaSaslMechanism = kafkaSaslMechanism; }

    public String getKafkaSaslJaasConfig() { return kafkaSaslJaasConfig; }
    public void setKafkaSaslJaasConfig(String kafkaSaslJaasConfig) { this.kafkaSaslJaasConfig = kafkaSaslJaasConfig; }

    public String getKafkaSslTruststoreLocation() { return kafkaSslTruststoreLocation; }
    public void setKafkaSslTruststoreLocation(String kafkaSslTruststoreLocation) { this.kafkaSslTruststoreLocation = kafkaSslTruststoreLocation; }

    public String getKafkaSslTruststorePassword() { return kafkaSslTruststorePassword; }
    public void setKafkaSslTruststorePassword(String kafkaSslTruststorePassword) { this.kafkaSslTruststorePassword = kafkaSslTruststorePassword; }

    public String getKafkaSslTruststoreType() { return kafkaSslTruststoreType; }
    public void setKafkaSslTruststoreType(String kafkaSslTruststoreType) { this.kafkaSslTruststoreType = kafkaSslTruststoreType; }

    public String getKafkaSslKeystoreLocation() { return kafkaSslKeystoreLocation; }
    public void setKafkaSslKeystoreLocation(String kafkaSslKeystoreLocation) { this.kafkaSslKeystoreLocation = kafkaSslKeystoreLocation; }

    public String getKafkaSslKeystorePassword() { return kafkaSslKeystorePassword; }
    public void setKafkaSslKeystorePassword(String kafkaSslKeystorePassword) { this.kafkaSslKeystorePassword = kafkaSslKeystorePassword; }

    public String getKafkaSslKeystoreType() { return kafkaSslKeystoreType; }
    public void setKafkaSslKeystoreType(String kafkaSslKeystoreType) { this.kafkaSslKeystoreType = kafkaSslKeystoreType; }

    public String getKafkaSslKeyPassword() { return kafkaSslKeyPassword; }
    public void setKafkaSslKeyPassword(String kafkaSslKeyPassword) { this.kafkaSslKeyPassword = kafkaSslKeyPassword; }

    public String getKafkaSslEndpointIdentificationAlgorithm() { return kafkaSslEndpointIdentificationAlgorithm; }
    public void setKafkaSslEndpointIdentificationAlgorithm(String value) { this.kafkaSslEndpointIdentificationAlgorithm = value; }

    public long getKafkaRetryInitialMs() { return kafkaRetryInitialMs; }
    public void setKafkaRetryInitialMs(long kafkaRetryInitialMs) { this.kafkaRetryInitialMs = kafkaRetryInitialMs; }

    public long getKafkaMaxRetries() { return kafkaMaxRetries; }
    public void setKafkaMaxRetries(long kafkaMaxRetries) { this.kafkaMaxRetries = kafkaMaxRetries; }

    public boolean isKafkaEnabled() { return kafkaEnabled; }
    public void setKafkaEnabled(boolean kafkaEnabled) { this.kafkaEnabled = kafkaEnabled; }

    public boolean isReconciliationEnabled() { return reconciliationEnabled; }
    public void setReconciliationEnabled(boolean reconciliationEnabled) { this.reconciliationEnabled = reconciliationEnabled; }

    public long getReconciliationIntervalMs() { return reconciliationIntervalMs; }
    public void setReconciliationIntervalMs(long reconciliationIntervalMs) { this.reconciliationIntervalMs = reconciliationIntervalMs; }

    public long getReconciliationInitialDelayMs() { return reconciliationInitialDelayMs; }
    public void setReconciliationInitialDelayMs(long reconciliationInitialDelayMs) { this.reconciliationInitialDelayMs = reconciliationInitialDelayMs; }

    public long getProcessedEventsRetentionDays() { return processedEventsRetentionDays; }
    public void setProcessedEventsRetentionDays(long processedEventsRetentionDays) { this.processedEventsRetentionDays = processedEventsRetentionDays; }

    public boolean isProcessedEventsCleanupEnabled() { return processedEventsCleanupEnabled; }
    public void setProcessedEventsCleanupEnabled(boolean processedEventsCleanupEnabled) { this.processedEventsCleanupEnabled = processedEventsCleanupEnabled; }

    public long getProcessedEventsCleanupIntervalMs() { return processedEventsCleanupIntervalMs; }
    public void setProcessedEventsCleanupIntervalMs(long processedEventsCleanupIntervalMs) { this.processedEventsCleanupIntervalMs = processedEventsCleanupIntervalMs; }

    public long getProcessedEventsCleanupInitialDelayMs() { return processedEventsCleanupInitialDelayMs; }
    public void setProcessedEventsCleanupInitialDelayMs(long processedEventsCleanupInitialDelayMs) { this.processedEventsCleanupInitialDelayMs = processedEventsCleanupInitialDelayMs; }

    public String getProvisioningStatus() { return provisioningStatus; }
    public void setProvisioningStatus(String provisioningStatus) { this.provisioningStatus = provisioningStatus; }
}
