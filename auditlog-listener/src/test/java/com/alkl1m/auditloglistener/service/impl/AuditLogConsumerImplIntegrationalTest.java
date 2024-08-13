package com.alkl1m.auditloglistener.service.impl;

import com.alkl1m.auditloglistener.entity.AuditLog;
import com.alkl1m.auditloglistener.payload.AuditLogEvent;
import com.alkl1m.auditloglistener.repository.AuditLogRepository;
import com.alkl1m.auditloglistener.util.KafkaContainerCluster;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

@Testcontainers
@SpringBootTest
class AuditLogConsumerImplIntegrationalTest {

    public static final String TOPIC_NAME_SEND_ORDER= "send-auditlog-event";

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static String bootstrapServers;

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:12")
            .withReuse(true);

    static {
        Startables.deepStart(Stream.of(postgreSQLContainer)).join();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", postgreSQLContainer::getDriverClassName);
        registry.add("spring.kafka.bootstrap-servers", () -> bootstrapServers);
    }

    @BeforeAll
    public static void setUp() {
        KafkaContainerCluster cluster = new KafkaContainerCluster("7.4.0", 3, 2);
        cluster.start();
        bootstrapServers = cluster.getBootstrapServers();
        System.setProperty("spring.kafka.bootstrap-servers", bootstrapServers);
    }

    @AfterEach
    public void destroy() {
        auditLogRepository.deleteAll();
    }

    @Test
    void testConsume_withValidPayload_returnsSavedData() throws InterruptedException, JsonProcessingException {
        String[] args = new String[]{"arg1", "arg2"};
        AuditLogEvent event = new AuditLogEvent("server1", "GET", args, "success", null);

        KafkaTemplate<String, String> kafkaTemplate = getKafkaTemplate(bootstrapServers);
        String jsonLogEntry = objectMapper.writeValueAsString(event);

        Thread.sleep(5000);
        kafkaTemplate.send(TOPIC_NAME_SEND_ORDER, jsonLogEntry);
        Thread.sleep(5000);

        List<AuditLog> auditLogs = auditLogRepository.findAll();

        for (AuditLog auditLog : auditLogs) {
            assertEquals(auditLog.getServerSource(), event.getServerSource());
            assertEquals(auditLog.getMethod(), event.getMethod());
            assertEquals(auditLog.getException(), event.getException());
        }
    }

    @Test
    void testConsume_withTwoAttempts_returnsSavedData() throws InterruptedException, JsonProcessingException {
        kafkaListenerEndpointRegistry.getListenerContainer("auditLogEvent").stop();
        String[] args = new String[]{"arg1", "arg2"};
        AuditLogEvent event = new AuditLogEvent("server1", "GET", args, "success", null);

        KafkaTemplate<String, String> kafkaTemplate = getKafkaTemplate(bootstrapServers);
        String jsonLogEntry = objectMapper.writeValueAsString(event);

        Thread.sleep(5000);
        kafkaTemplate.send(TOPIC_NAME_SEND_ORDER, jsonLogEntry);
        Thread.sleep(5000);

        kafkaListenerEndpointRegistry.getListenerContainer("auditLogEvent").start();

        Thread.sleep(5000);

        List<AuditLog> auditLogs = auditLogRepository.findAll();

        for (AuditLog auditLog : auditLogs) {
            assertEquals(auditLog.getServerSource(), event.getServerSource());
            assertEquals(auditLog.getMethod(), event.getMethod());
            assertEquals(auditLog.getException(), event.getException());
        }
    }

    @Test
    void testConsume_withThreeEntriesAndTwoAttempts_returnsSavedDataInValidOrder() throws InterruptedException, JsonProcessingException {
        kafkaListenerEndpointRegistry.getListenerContainer("auditLogEvent").stop();
        String[] args = new String[]{"arg1", "arg2"};
        AuditLogEvent event1 = new AuditLogEvent("server1", "GET", args, "success", null);
        AuditLogEvent event2 = new AuditLogEvent("server2", "GET", args, "success", null);
        AuditLogEvent event3 = new AuditLogEvent("server3", "GET", args, "success", null);

        KafkaTemplate<String, String> kafkaTemplate = getKafkaTemplate(bootstrapServers);
        String jsonLogEntry1 = objectMapper.writeValueAsString(event1);
        String jsonLogEntry2 = objectMapper.writeValueAsString(event2);
        String jsonLogEntry3 = objectMapper.writeValueAsString(event3);

        Thread.sleep(5000);
        kafkaTemplate.send(TOPIC_NAME_SEND_ORDER, jsonLogEntry1);
        kafkaTemplate.send(TOPIC_NAME_SEND_ORDER, jsonLogEntry3);
        kafkaTemplate.send(TOPIC_NAME_SEND_ORDER, jsonLogEntry2);
        Thread.sleep(5000);

        kafkaListenerEndpointRegistry.getListenerContainer("auditLogEvent").start();

        Thread.sleep(5000);

        List<AuditLog> auditLogs = auditLogRepository.findAll();

        assertEquals(3, auditLogs.size());
    }

    @NotNull
    private static KafkaTemplate<String, String> getKafkaTemplate(String bootstrapServers) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(configProps);
        return new KafkaTemplate<>(producerFactory);
    }
}