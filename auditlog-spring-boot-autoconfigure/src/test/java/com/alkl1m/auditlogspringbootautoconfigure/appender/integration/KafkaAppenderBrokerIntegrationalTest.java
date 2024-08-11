package com.alkl1m.auditlogspringbootautoconfigure.appender.integration;

import com.alkl1m.auditlogspringbootautoconfigure.domain.AuditLogEntry;
import com.alkl1m.auditlogspringbootautoconfigure.util.IntegrationTestUtils;
import com.alkl1m.auditlogspringbootautoconfigure.util.KafkaContainerCluster;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class KafkaAppenderBrokerIntegrationalTest {

    public static final String TOPIC_NAME_SEND_ORDER= "send-auditlog-event";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static String bootstrapServers;
    private static KafkaContainerCluster isolatedCluster;

    @BeforeAll
    public static void setUp() {
        isolatedCluster = new KafkaContainerCluster("latest", 1, 1);
        isolatedCluster.start();
        bootstrapServers = isolatedCluster.getBootstrapServers();
    }

    @Test
    void testProduce_withLaggedBroker_returnsSavedData() throws IOException, InterruptedException {
        Object[] args = new Object[]{"arg3", "arg4"};
        AuditLogEntry entry = new AuditLogEntry("server2", "GET", args, "success", null);
        LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(new SimpleMessage(objectMapper.writeValueAsString(entry)))
                .build();

        Property[] kafkaProperties = IntegrationTestUtils.createKafkaProperties(isolatedCluster.getBootstrapServers());

        KafkaConsumer<String, AuditLogEntry> consumer = new KafkaConsumer<>(ImmutableMap.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "test-group-id",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(),
                new JsonDeserializer<>(AuditLogEntry.class));

        Appender kafkaAppender = IntegrationTestUtils.createKafkaAppender(kafkaProperties, TOPIC_NAME_SEND_ORDER);

        consumer.subscribe(Collections.singletonList(TOPIC_NAME_SEND_ORDER));

        kafkaAppender.start();

        IntegrationTestUtils.pauseKafkaBrokers(isolatedCluster);

        Thread.sleep(10000);

        Thread appendThread = new Thread(() -> kafkaAppender.append(event));
        Thread unlockThread = new Thread(() -> IntegrationTestUtils.unpauseKafkaBrokersAfterDelay(isolatedCluster));

        appendThread.start();
        unlockThread.start();

        appendThread.join();
        unlockThread.join();

        ConsumerRecords<String, AuditLogEntry> records = consumer.poll(Duration.ofMillis(10000L));
        consumer.close();

        assertEquals(1, records.count());
        for (ConsumerRecord<String, AuditLogEntry> record : records) {
            assertEquals(record.value().getServerSource(), entry.getServerSource());
            assertEquals(record.value().getResult(), entry.getResult());
            assertEquals(record.value().getException(), entry.getException());
        }
    }

}
