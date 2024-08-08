package com.alkl1m.auditlogspringbootautoconfigure.appender;

import com.alkl1m.auditlogspringbootautoconfigure.domain.AuditLogEntry;
import com.alkl1m.auditlogspringbootautoconfigure.util.KafkaContainerCluster;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class KafkaAppenderIntegrationalTest {

    public static final String TOPIC_NAME_SEND_ORDER= "send-auditlog-event";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static String bootstrapServers;

    @BeforeAll
    public static void setUp() {
        KafkaContainerCluster cluster = new KafkaContainerCluster("7.4.0", 3, 2);
        cluster.start();
        bootstrapServers = cluster.getBootstrapServers();
    }

    @Test
    void testProduce_withValidPayload_returnsSavedData() throws JsonProcessingException {
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

    @Test
    void testProduce_withValidPayload_returnsSavedDataOnAllReplicas() throws JsonProcessingException {
        Object[] args = new Object[]{"arg1", "arg2"};
        AuditLogEntry entry = new AuditLogEntry("server1", "GET", args, "success", null);
        LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(new SimpleMessage(objectMapper.writeValueAsString(entry)))
                .build();

        Property[] kafkaProperties = createKafkaProperties(bootstrapServers);

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(ImmutableMap.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "test-group-id",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(),
                new StringDeserializer());

        Appender kafkaAppender = createKafkaAppender(kafkaProperties);

        consumer.subscribe(Collections.singletonList(TOPIC_NAME_SEND_ORDER));

        kafkaAppender.start();
        kafkaAppender.append(event);

        Map<Integer, List<String>> partitionMessages = new HashMap<>();

        for (int i = 0; i < 2; i++) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            records.forEach(record -> {
                partitionMessages.computeIfAbsent(record.partition(), k -> new ArrayList<>()).add(record.value());
            });
        }

        for (int partition : partitionMessages.keySet()) {
            assertThat(partitionMessages.get(partition)).isNotEmpty();
        }

        consumer.close();
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