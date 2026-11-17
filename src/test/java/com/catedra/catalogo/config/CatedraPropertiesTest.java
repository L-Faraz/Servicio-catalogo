package com.catedra.catalogo.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatedraPropertiesTest {

    @Test
    void derivesKafkaTopicConsumerGroupAndWriteNamespaceFromGroupId() {
        CatedraProperties properties = new CatedraProperties();
        properties.setGroupId("proyecto-juan-perez");

        assertThat(properties.getEffectiveKafkaCatalogTopic())
                .isEqualTo("catedra.catalog.proyecto-juan-perez");
        assertThat(properties.getEffectiveKafkaConsumerGroupId())
                .isEqualTo("alumnos-proyecto-juan-perez");
        assertThat(properties.getEffectiveRedisWriteNamespace())
                .isEqualTo("alumnos:proyecto-juan-perez:*");
    }

    @Test
    void explicitIntegrationValuesTakePrecedence() {
        CatedraProperties properties = new CatedraProperties();
        properties.setGroupId("proyecto-juan-perez");
        properties.setKafkaCatalogTopic("catedra.catalog.custom");
        properties.setKafkaConsumerGroupId("custom-consumer-group");
        properties.setRedisWriteNamespace("alumnos:custom:*");

        assertThat(properties.getEffectiveKafkaCatalogTopic()).isEqualTo("catedra.catalog.custom");
        assertThat(properties.getEffectiveKafkaConsumerGroupId()).isEqualTo("custom-consumer-group");
        assertThat(properties.getEffectiveRedisWriteNamespace()).isEqualTo("alumnos:custom:*");
    }

    @Test
    void doesNotInventTopicOrWriteNamespaceWithoutGroupId() {
        CatedraProperties properties = new CatedraProperties();

        assertThat(properties.getEffectiveKafkaCatalogTopic()).isNull();
        assertThat(properties.getEffectiveKafkaConsumerGroupId()).isEqualTo("servicio-catalogo-group");
        assertThat(properties.getEffectiveRedisWriteNamespace()).isNull();
    }

    @Test
    void onlyProvisionedStatusIsConsideredReady() {
        CatedraProperties properties = new CatedraProperties();

        assertThat(properties.isProvisioned()).isFalse();
        assertThat(properties.isProvisioningBlocked()).isFalse();

        properties.setProvisioningStatus("PROVISIONED");
        assertThat(properties.isProvisioned()).isTrue();
        assertThat(properties.isProvisioningBlocked()).isFalse();

        properties.setProvisioningStatus("PENDING");
        assertThat(properties.isProvisioned()).isFalse();
        assertThat(properties.isProvisioningBlocked()).isTrue();

        properties.setProvisioningStatus("FAILED");
        assertThat(properties.isProvisioningBlocked()).isTrue();

        properties.setProvisioningStatus("REVOKED");
        assertThat(properties.isProvisioningBlocked()).isTrue();
    }
}
