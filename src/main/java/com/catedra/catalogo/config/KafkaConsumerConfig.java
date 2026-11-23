package com.catedra.catalogo.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.StringUtils;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@ConditionalOnProperty(name = "catedra.integration.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConsumerConfig {

    private final CatedraProperties properties;

    public KafkaConsumerConfig(CatedraProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = securityConfig();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getEffectiveKafkaConsumerGroupId());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ProducerFactory<String, String> kafkaProducerFactory() {
        Map<String, Object> config = securityConfig();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(kafkaProducerFactory());
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate(),
                (record, exception) -> new TopicPartition(
                        properties.getEffectiveKafkaDltTopic(), record.partition())
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(
                properties.getKafkaRetryInitialMs(), properties.getKafkaMaxRetries()));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }

    private String bootstrapServers() {
        String servers = properties.getKafkaBootstrapServers();
        if (!StringUtils.hasText(servers)) {
            throw new IllegalStateException("catedra.integration.kafka-bootstrap-servers es obligatorio para usar Kafka");
        }
        return servers;
    }

    private Map<String, Object> securityConfig() {
        Map<String, Object> config = new HashMap<>();
        String protocol = properties.getKafkaSecurityProtocol();
        config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, protocol);

        if (protocol != null && protocol.contains("SASL")) {
            putIfPresent(config, SaslConfigs.SASL_MECHANISM, properties.getKafkaSaslMechanism());
            putIfPresent(config, SaslConfigs.SASL_JAAS_CONFIG, properties.getKafkaSaslJaasConfig());
        }
        if (protocol != null && protocol.contains("SSL")) {
            putIfPresent(config, SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, properties.getKafkaSslTruststoreLocation());
            putIfPresent(config, SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, properties.getKafkaSslTruststorePassword());
            putIfPresent(config, SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, properties.getKafkaSslTruststoreType());
            putIfPresent(config, SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, properties.getKafkaSslKeystoreLocation());
            putIfPresent(config, SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, properties.getKafkaSslKeystorePassword());
            putIfPresent(config, SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, properties.getKafkaSslKeystoreType());
            putIfPresent(config, SslConfigs.SSL_KEY_PASSWORD_CONFIG, properties.getKafkaSslKeyPassword());
            putIfPresent(config, SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
                    properties.getKafkaSslEndpointIdentificationAlgorithm());
        }

        return config;
    }

    private void putIfPresent(Map<String, Object> config, String key, String value) {
        if (StringUtils.hasText(value)) {
            config.put(key, value);
        }
    }
}
