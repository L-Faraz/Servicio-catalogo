package com.catedra.catalogo.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ConsumerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConsumerConfigTest {

    @Test
    void consumerGroupIsDerivedFromGroupId() {
        CatedraProperties properties = new CatedraProperties();
        properties.setGroupId("proyecto-juan-perez");

        ConsumerFactory<String, String> factory = new KafkaConsumerConfig(properties).consumerFactory();

        assertThat(factory.getConfigurationProperties())
                .containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "alumnos-proyecto-juan-perez");
    }

    @Test
    void explicitConsumerGroupTakesPrecedence() {
        CatedraProperties properties = new CatedraProperties();
        properties.setGroupId("proyecto-juan-perez");
        properties.setKafkaConsumerGroupId("custom-consumer-group");

        ConsumerFactory<String, String> factory = new KafkaConsumerConfig(properties).consumerFactory();

        assertThat(factory.getConfigurationProperties())
                .containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "custom-consumer-group");
    }
}
