package com.alkl1m.auditloglistener.service;

import com.alkl1m.auditloglistener.payload.AuditLogEvent;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;

public interface AuditLogConsumer {

    void consume(@Payload AuditLogEvent auditLogEvent, Acknowledgment acknowledgment);

}
