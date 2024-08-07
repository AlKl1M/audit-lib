package com.alkl1m.auditlogspringbootautoconfigure.appender;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaAppenderTest {

    private KafkaProducer<String, String> producer;
    private KafkaAppender kafkaAppender;

    @BeforeEach
    void setUp() {
        producer = mock(KafkaProducer.class);
        String topic = "test-topic";
        Layout<? extends Serializable> layout = JsonLayout.createDefaultLayout();
        kafkaAppender = new KafkaAppender("KafkaAppender", null, layout, true, producer, topic);
        kafkaAppender.start();
    }

    @Test
    void testAppend_withValidPayload_returnsValidData() {
        LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(new SimpleMessage("Test message"))
                .build();

        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test-topic", 0), 0, 0, System.currentTimeMillis(), null, 0, 0);

        Future<RecordMetadata> future = CompletableFuture.completedFuture(metadata);
        when(producer.send(any(ProducerRecord.class))).thenReturn(future);

        kafkaAppender.append(event);

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(captor.capture());

        assertEquals("Test message", captor.getValue().value());
    }

    @Test
    void testAppend_withWrongPayload_throwsException() {
        LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(new SimpleMessage("Test message"))
                .build();

        when(producer.send(any(ProducerRecord.class))).thenThrow(new RuntimeException("Kafka error"));

        Exception exception = assertThrows(AppenderLoggingException.class, () -> {
            kafkaAppender.append(event);
        });

        assertEquals("Не получается передать сообщения в кафку в аппендере: Kafka error", exception.getMessage());
    }

    @Test
    void testStop_withValidPayload_returnsValidData() {
        kafkaAppender.stop();
        verify(producer).close();
    }
}