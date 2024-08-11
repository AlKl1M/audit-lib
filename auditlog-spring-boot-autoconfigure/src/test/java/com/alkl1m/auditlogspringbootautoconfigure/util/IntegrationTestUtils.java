package com.alkl1m.auditlogspringbootautoconfigure.util;

import com.alkl1m.auditlogspringbootautoconfigure.appender.KafkaAppender;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.Property;

public class IntegrationTestUtils {

    public static Property[] createKafkaProperties(String bootstrapServers) {
        return new Property[]{
                Property.createProperty(ProducerConfig.ACKS_CONFIG, "all"),
                Property.createProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true"),
                Property.createProperty(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "auditlog-id"),
                Property.createProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
                Property.createProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer"),
                Property.createProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer"),
        };
    }

    public static Appender createKafkaAppender(Property[] kafkaProperties, String topicName) {
        return KafkaAppender.createAppender(
                "KafkaAppender",
                null,
                null,
                topicName,
                null,
                kafkaProperties
        );
    }

}
