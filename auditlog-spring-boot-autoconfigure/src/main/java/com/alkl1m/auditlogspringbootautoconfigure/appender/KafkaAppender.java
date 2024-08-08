package com.alkl1m.auditlogspringbootautoconfigure.appender;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
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
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.core.util.Booleans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Кастомный appender, который отправляет логи в кафку.
 *
 * @author alkl1m
 */
@Plugin(name = "KafkaAppender", category = "Core", elementType = "appender", printObject = true)
public class KafkaAppender extends AbstractAppender {

    private final KafkaProducer<String, String> producer;
    private final String topic;

    protected KafkaAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions,
                            KafkaProducer<String, String> producer, String topic) {
        super(name, filter, layout, ignoreExceptions);
        this.producer = producer;
        this.topic = topic;
    }

    /**
     * Фабрика для KafkaAppender.
     *
     * @param name имя KafkaAppender.
     * @param filter фильтр KafkaAppender.
     * @param ignore флаг для игнора исключений.
     * @param topic топик, куда будут отправляться сообщения.
     * @param layout схема отображения логов.
     * @param properties список свойств для кафки.
     * @return appender для отправки логов в кафку.
     */
    @PluginFactory
    public static KafkaAppender createAppender(@PluginAttribute("name") String name,
                                               @PluginElement("Filter") Filter filter,
                                               @PluginAttribute("ignoreExceptions") String ignore,
                                               @PluginAttribute("topic") String topic,
                                               @PluginElement("Layout") Layout<? extends Serializable> layout,
                                               @PluginElement("Properties") Property[] properties) {
        boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);

        Map<String, Object> props = Arrays.stream(properties)
                .collect(Collectors.toMap(Property::getName, Property::getValue));

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        producer.initTransactions();

        Layout<? extends Serializable> effectiveLayout = layout != null ? layout : JsonLayout.createDefaultLayout();

        return new KafkaAppender(name, filter, effectiveLayout, ignoreExceptions, producer, topic);
    }

    /**
     * Метод для остановки.
     */
    @Override
    public final void stop() {
        super.stop();
        if (producer != null) {
            producer.close();
        }
    }

    /**
     * Метод, который отправляет в кафку сообщение внутри транзакции.
     *
     * @param event тело лога.
     */
    public void append(LogEvent event) {
        if (producer != null) {
            try {
                producer.beginTransaction();
                String message = event.getMessage().getFormattedMessage();
                producer.send(new ProducerRecord<>(topic, message));
                producer.commitTransaction();
            } catch (Exception e) {
                LOGGER.error("Не получается передать сообщения в кафку в аппендере [{}].", getName(), e);
                producer.abortTransaction();
                throw new AppenderLoggingException("Не получается передать сообщения в кафку в аппендере: " + e.getMessage(), e);
            }
        }
    }
}

