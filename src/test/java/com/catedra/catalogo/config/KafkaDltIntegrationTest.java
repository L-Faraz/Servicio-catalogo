package com.catedra.catalogo.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = KafkaDltIntegrationTest.TestConfig.class)
@EmbeddedKafka(topics = {KafkaDltIntegrationTest.SOURCE_TOPIC, KafkaDltIntegrationTest.DLT_TOPIC}, partitions = 1)
class KafkaDltIntegrationTest {

    static final String SOURCE_TOPIC = "catalog-source";
    static final String DLT_TOPIC = "catalog-source.dlt";

    @Autowired
    private org.springframework.kafka.test.EmbeddedKafkaBroker broker;

    @Test
    void failedRecordIsPublishedToDeadLetterTopic() {
        KafkaTemplate<String, String> template = kafkaTemplate();
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (record, exception) -> new TopicPartition(DLT_TOPIC, record.partition())
        );

        ConsumerRecord<String, String> failedRecord =
                new ConsumerRecord<>(SOURCE_TOPIC, 0, 0L, "event-1", "payload-invalido");
        recoverer.accept(failedRecord, new IllegalStateException("sin version objetivo"));

        Consumer<String, String> consumer = consumer();
        try {
            ConsumerRecord<String, String> dltRecord = KafkaTestUtils.getSingleRecord(consumer, DLT_TOPIC);
            assertThat(dltRecord.key()).isEqualTo("event-1");
            assertThat(dltRecord.value()).isEqualTo("payload-invalido");
        } finally {
            consumer.close();
            template.destroy();
        }
    }

    private KafkaTemplate<String, String> kafkaTemplate() {
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    private Consumer<String, String> consumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps("catalog-dlt-test", "false", broker);
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer()).createConsumer();
        broker.consumeFromAnEmbeddedTopic(consumer, DLT_TOPIC);
        return consumer;
    }

    @Configuration
    static class TestConfig {
    }
}
