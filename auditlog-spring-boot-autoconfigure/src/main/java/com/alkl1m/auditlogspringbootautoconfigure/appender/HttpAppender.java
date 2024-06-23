package com.alkl1m.auditlogspringbootautoconfigure.appender;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Кастомный аппендер, который записывает логи в файл.
 * Файл лежит в корне проекта (logs/log4j.log).
 *
 * @author alkl1m
 */
@Plugin(name = "HttpAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class HttpAppender extends AbstractAppender {

    private ConcurrentMap<String, LogEvent> eventMap = new ConcurrentHashMap<>();

    protected HttpAppender(String name, Filter filter) {
        super(name, filter, null);
    }

    /**
     * Фабричный метод для создания нового экземпляра HttpAppender.
     *
     * @param name имя нового экземпляра.
     * @param filter объект для фильтрации событий логирования перед их отправкой.
     * @return экземпляр HttpAppender.
     */
    @PluginFactory
    public static HttpAppender createAppender(@PluginAttribute("name") String name, @PluginElement("Filter") final Filter filter) {
        return new HttpAppender(name, filter);
    }

    /**
     * Метод для добавления нового события логирования в лог-файл.
     *
     * @param event информация о событии логирования (уровень, сообщение, метаданные).
     */
    @Override
    public void append(LogEvent event) {
        String logFilePath = "logs/log4j.log";

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(logFilePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(Instant.now() + " " + event.getSource() + ": " + event.getMessage());
            writer.newLine();
        } catch (IOException e) {
            error("Can't create a file for logs");
        }
    }

    /**
     * @return карту событий логирования.
     */
    public ConcurrentMap<String, LogEvent> getEventMap() {
        return eventMap;
    }

    /**
     * @param eventMap карта событий логирования.
     */
    public void setEventMap(ConcurrentMap<String, LogEvent> eventMap) {
        this.eventMap = eventMap;
    }

}
