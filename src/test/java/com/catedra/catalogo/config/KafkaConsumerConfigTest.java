package com.catedra.catalogo.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConsumerConfigTest {

    @Test
    void consumerGroupIsDerivedFromGroupId() {
        CatedraProperties properties = new CatedraProperties();
        properties.setGroupId("proyecto-juan-perez");

        ConsumerFactory<String, String> factory = new KafkaConsumerConfig(properties).consumerFactory();

        assertThat(factory.getConfigurationProperties())
                .containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "alumnos-proyecto-juan-perez")
                .containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT");
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

    @Test
    void sslAndSaslPropertiesArePropagatedToConsumerAndProducer() {
        CatedraProperties properties = new CatedraProperties();
        properties.setKafkaBootstrapServers("localhost:9094");
        properties.setKafkaSecurityProtocol("SASL_SSL");
        properties.setKafkaSaslMechanism("PLAIN");
        properties.setKafkaSaslJaasConfig("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"u\" password=\"p\";");
        properties.setKafkaSslTruststoreLocation("/etc/kafka/truststore.jks");
        properties.setKafkaSslTruststorePassword("trust-secret");
        properties.setKafkaSslTruststoreType("JKS");
        KafkaConsumerConfig config = new KafkaConsumerConfig(properties);

        assertThat(config.consumerFactory().getConfigurationProperties())
                .containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                .containsEntry(SaslConfigs.SASL_MECHANISM, "PLAIN")
                .containsEntry(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/etc/kafka/truststore.jks")
                .containsEntry(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "JKS");
        ProducerFactory<String, String> producerFactory = config.kafkaProducerFactory();
        assertThat(producerFactory.getConfigurationProperties())
                .containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                .containsEntry(ProducerConfig.ACKS_CONFIG, "all");
    }

    @Test
    void dltTopicIsDerivedFromCatalogTopicAndCanBeOverridden() {
        CatedraProperties properties = new CatedraProperties();
        properties.setGroupId("proyecto-juan-perez");
        assertThat(properties.getEffectiveKafkaDltTopic())
                .isEqualTo("catedra.catalog.proyecto-juan-perez.dlt");

        properties.setKafkaDltTopic("custom-dlt");
        assertThat(properties.getEffectiveKafkaDltTopic()).isEqualTo("custom-dlt");
    }
}
