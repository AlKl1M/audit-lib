package com.alkl1m.auditlogspringbootautoconfigure.appender;

import com.alkl1m.auditlogspringbootautoconfigure.domain.AuditLogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class KafkaAppenderIntegrationalTest {

    public static final String TOPIC_NAME_SEND_ORDER= "send-auditlog-event";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Container
    static final KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.4"))
                    .withEmbeddedZookeeper()
                    .withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:9093 ,BROKER://0.0.0.0:9092")
                    .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT")
                    .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER")
                    .withEnv("KAFKA_BROKER_ID", "1")
                    .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                    .withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1")
                    .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                    .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
                    .withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "")
                    .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");


    static {
        Startables.deepStart(Stream.of(kafkaContainer)).join();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Test
    void testProduce_withValidPayload_returnsSavedData() throws JsonProcessingException {
        String bootstrapServers = kafkaContainer.getBootstrapServers();
        Object[] args = new Object[]{"arg1", "arg2"};
        AuditLogEntry entry = new AuditLogEntry("server1", "GET", args, "success", null);
        LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(new SimpleMessage(objectMapper.writeValueAsString(entry)))
                .build();

        Property[] kafkaProperties = createKafkaProperties(bootstrapServers);

        KafkaConsumer<String, AuditLogEntry> consumer = new KafkaConsumer<>(ImmutableMap.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "test-group-id",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(),
                new JsonDeserializer<>(AuditLogEntry.class));

        Appender kafkaAppender = createKafkaAppender(kafkaProperties);

        consumer.subscribe(Collections.singletonList(TOPIC_NAME_SEND_ORDER));

        kafkaAppender.start();
        kafkaAppender.append(event);

        ConsumerRecords<String, AuditLogEntry> records = consumer.poll(Duration.ofMillis(10000L));
        consumer.close();

        assertEquals(1, records.count());
        for (ConsumerRecord<String, AuditLogEntry> record : records) {
            assertEquals(record.value().getServerSource(), entry.getServerSource());
            assertEquals(record.value().getResult(), entry.getResult());
            assertEquals(record.value().getException(), entry.getException());
        }
    }

    private Property[] createKafkaProperties(String bootstrapServers) {
        return new Property[]{
                Property.createProperty(ProducerConfig.ACKS_CONFIG, "all"),
                Property.createProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true"),
                Property.createProperty(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "auditlog-id"),
                Property.createProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
                Property.createProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer"),
                Property.createProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer"),
        };
    }

    private Appender createKafkaAppender(Property[] kafkaProperties) {
        return KafkaAppender.createAppender(
                "KafkaAppender",
                null,
                null,
                TOPIC_NAME_SEND_ORDER,
                null,
                kafkaProperties
        );
    }

}