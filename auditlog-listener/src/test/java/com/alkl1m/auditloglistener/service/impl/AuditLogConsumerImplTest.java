package com.alkl1m.auditloglistener.service.impl;

import com.alkl1m.auditloglistener.entity.AuditLog;
import com.alkl1m.auditloglistener.payload.AuditLogEvent;
import com.alkl1m.auditloglistener.repository.AuditLogRepository;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogConsumerImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @InjectMocks
    private AuditLogConsumerImpl auditLogConsumer;

    private Acknowledgment acknowledgment;

    @BeforeEach
    public void setUp() {
        acknowledgment = mock(Acknowledgment.class);
    }

    @Test
    void testConsume_withValidAuditLogEvent_returnsValidData() {
        Object[] args = new Object[]{"arg1", "arg2"};
        AuditLogEvent event = new AuditLogEvent("server1", "GET", args, "success", null);

        auditLogConsumer.consume(event, acknowledgment);

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        verify(acknowledgment).acknowledge();

        AuditLog savedAuditLog = auditLogCaptor.getValue();
        assertEquals("server1", savedAuditLog.getServerSource());
        assertEquals("GET", savedAuditLog.getMethod());
        assertEquals("success", savedAuditLog.getResult());
        assertNull(savedAuditLog.getException());
    }

}