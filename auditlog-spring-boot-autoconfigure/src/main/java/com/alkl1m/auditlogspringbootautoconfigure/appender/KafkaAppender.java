package com.alkl1m.auditlogspringbootautoconfigure.appender;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.apache.logging.log4j.core.util.Booleans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Plugin(name = "Kafka", category = "Core", elementType = "appender", printObject = true)
public class KafkaAppender extends AbstractAppender {

    private final KafkaProducer<String, String> producer;
    private final String topic;
    private final boolean syncSend;

    protected KafkaAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions,
                            KafkaProducer<String, String> producer, String topic, boolean syncSend) {
        super(name, filter, layout, ignoreExceptions);
        this.producer = producer;
        this.topic = topic;
        this.syncSend = syncSend;
    }

    @PluginFactory
    public static KafkaAppender createAppender(@PluginAttribute("name") String name,
                                               @PluginElement("Filter") Filter filter,
                                               @PluginAttribute("ignoreExceptions") String ignore,
                                               @PluginAttribute("topic") String topic,
                                               @PluginAttribute("enable") String enable,
                                               @PluginAttribute("syncsend") String syncSend,
                                               @PluginElement("Layout") Layout<? extends Serializable> layout,
                                               @PluginElement("Properties") Property[] properties) {
        boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        boolean enableKafka = Booleans.parseBoolean(enable, true);
        boolean sync = Booleans.parseBoolean(syncSend, false);

        Map<String, Object> props = Arrays.stream(properties)
                .collect(Collectors.toMap(Property::getName, Property::getValue));

        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = enableKafka ? new KafkaProducer<>(props) : null;
        Layout<? extends Serializable> effectiveLayout = layout != null ? layout : SerializedLayout.createLayout();

        return new KafkaAppender(name, filter, effectiveLayout, ignoreExceptions, producer, topic, sync);
    }

    @Override
    public final void stop() {
        super.stop();
        if (producer != null) {
            producer.close();
        }
    }

    public void append(LogEvent event) {
        if (producer != null) {
            try {
                Future<RecordMetadata> result = producer.send(new ProducerRecord<>(topic, getLayout().toSerializable(event).toString()));
                if (syncSend) {
                    result.get();
                }
            } catch (Exception e) {
                LOGGER.error("Unable to write to kafka for appender [{}].", getName(), e);
                throw new AppenderLoggingException("Unable to write to kafka in appender: " + e.getMessage(), e);
            }
        }
    }
}

