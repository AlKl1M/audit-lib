package com.alkl1m.auditloglistener.service.impl;

import com.alkl1m.auditloglistener.entity.AuditLog;
import com.alkl1m.auditloglistener.payload.AuditLogEvent;
import com.alkl1m.auditloglistener.repository.AuditLogRepository;
import com.alkl1m.auditloglistener.service.AuditLogConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * Сервис для работы с AuditLog.
 *
 * @author alkl1m
 */
@Service
@RequiredArgsConstructor
public class AuditLogConsumerImpl implements AuditLogConsumer {

    private final AuditLogRepository auditLogRepository;

    /**
     * Метод для обработки сообщения из Kafka-топика send-auditlog-event.
     *
     * @param auditLogEvent событие, содержащее информацию об аудите.
     * @param acknowledgment объект для ручного коммита.
     */
    @Override
    @KafkaListener(id = "auditLogEvent", topics = "send-auditlog-event", groupId = "group-1")
    @Transactional("transactionManager")
    public void consume(@Payload AuditLogEvent auditLogEvent,
                        Acknowledgment acknowledgment) {
        AuditLog auditLog = AuditLog.builder()
                .serverSource(auditLogEvent.getServerSource())
                .method(auditLogEvent.getMethod())
                .args(Arrays.toString(auditLogEvent.getArgs()))
                .result(auditLogEvent.getResult().toString())
                .exception(auditLogEvent.getException())
                .build();
        auditLogRepository.save(auditLog);
        acknowledgment.acknowledge();
    }
}