package com.alkl1m.auditloglistener.service;

import com.alkl1m.auditloglistener.entity.AuditLog;
import com.alkl1m.auditloglistener.payload.AuditLogEvent;
import com.alkl1m.auditloglistener.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class KafkaMessageConsumer {

    private final AuditLogRepository auditLogRepository;

    @KafkaListener(topics = "send-auditlog-event", groupId = "group-1")
    public void consume(AuditLogEvent logMessage) {

        AuditLog auditLog = AuditLog.builder()
                .serverSource(logMessage.getServerSource())
                .method(logMessage.getMethod())
                .args(Arrays.toString(logMessage.getArgs()))
                .result(logMessage.getResult().toString())
                .exception(logMessage.getException())
                .build();

        auditLogRepository.save(auditLog);
    }
}