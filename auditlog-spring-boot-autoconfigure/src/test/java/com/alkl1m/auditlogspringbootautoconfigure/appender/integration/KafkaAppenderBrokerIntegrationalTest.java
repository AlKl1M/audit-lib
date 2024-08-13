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
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Testcontainers
public class KafkaAppenderBrokerIntegrationalTest {

    public static final String TOPIC_NAME_SEND_ORDER= "send-auditlog-event";
    private static final int NUM_BROKERS = 3;
    private static final int NUM_TOPIC_PARTITIONS = 2;
    private static final KafkaContainerCluster CLUSTER = new KafkaContainerCluster("latest", NUM_BROKERS, NUM_TOPIC_PARTITIONS);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static String bootstrapServers;

    @BeforeAll
    public static void setUp() {
        CLUSTER.start();
        bootstrapServers = CLUSTER.getBootstrapServers();
    }

    @Test
    void testProduce_withLaggedBroker_returnsSavedData() throws IOException, InterruptedException {
        Object[] args = new Object[]{"arg3", "arg4"};
        AuditLogEntry entry = new AuditLogEntry("server2", "GET", args, "success", null);
        LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(new SimpleMessage(objectMapper.writeValueAsString(entry)))
                .build();

        Property[] kafkaProperties = IntegrationTestUtils.createKafkaProperties(CLUSTER.getBootstrapServers());

        KafkaConsumer<String, AuditLogEntry> consumer = new KafkaConsumer<>(ImmutableMap.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "test-group-id",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(),
                new JsonDeserializer<>(AuditLogEntry.class));

        Appender kafkaAppender = IntegrationTestUtils.createKafkaAppender(kafkaProperties, TOPIC_NAME_SEND_ORDER);

        consumer.subscribe(Collections.singletonList(TOPIC_NAME_SEND_ORDER));

        kafkaAppender.start();

        pauseKafkaBrokers(CLUSTER);

        await().atMost(10, SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, AuditLogEntry> records = consumer.poll(Duration.ofMillis(100));
            assertEquals(0, records.count());
        });

        Thread appendThread = new Thread(() -> kafkaAppender.append(event));
        Thread unlockThread = new Thread(() -> unpauseKafkaBrokersAfterDelay(CLUSTER));

        appendThread.start();
        unlockThread.start();

        appendThread.join();
        unlockThread.join();

        await().atMost(10, SECONDS).untilAsserted(() -> {
            ConsumerRecords<String, AuditLogEntry> records = consumer.poll(Duration.ofMillis(10000L));
            assertEquals(1, records.count());
            for (ConsumerRecord<String, AuditLogEntry> record : records) {
                assertEquals(record.value().getServerSource(), entry.getServerSource());
                assertEquals(record.value().getResult(), entry.getResult());
                assertEquals(record.value().getException(), entry.getException());
            }
        });

        consumer.close();
    }

    private void pauseKafkaBrokers(KafkaContainerCluster cluster) throws InterruptedException {
        for (KafkaContainer appender : cluster.getBrokers()) {
            appender.getDockerClient().pauseContainerCmd(appender.getContainerId()).exec();
        }
        Thread.sleep(10000);
    }

    private void unpauseKafkaBrokersAfterDelay(KafkaContainerCluster cluster) {
        try {
            Thread.sleep(10000);
            for (KafkaContainer appender : cluster.getBrokers()) {
                appender.getDockerClient().unpauseContainerCmd(appender.getContainerId()).exec();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
