package com.alkl1m.auditlogspringbootautoconfigure.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Advice для логирования HTTP-запросов.
 *
 * @author alkl1m
 */
@ControllerAdvice
public class HttpRequestLoggingAdvice implements RequestBodyAdvice {

    private static final Logger logger = LogManager.getLogger(HttpRequestLoggingAdvice.class);

    /**
     * Проверка поддержки метода.
     *
     * @param methodParameter параметр метода.
     * @param targetType тип, к которому должно быть преобразовано тело запроса.
     * @param converterType тип конвертера, который преобразует тело запроса в объект.
     * @return true, если метод поддерживается; иначе false.
     */
    @Override
    public boolean supports(@NonNull MethodParameter methodParameter,
                            @NonNull Type targetType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /**
     * @param inputMessage входящее http-сообщение.
     * @param parameter параметр метода контроллера, которому применена аннотация.
     * @param targetType тип, к которому должно быть преобразовано тело запроса.
     * @param converterType тип конвертера, который преобразует тело запроса в объект.
     * @return входящее http-сообщение.
     * @throws IOException вероятная ошибка ввода-вывода.
     */

    @Override
    public HttpInputMessage beforeBodyRead(@NonNull HttpInputMessage inputMessage,
                                           @NonNull MethodParameter parameter,
                                           @NonNull Type targetType,
                                           @NonNull Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        return inputMessage;
    }

    /**
     * Метод логирует RequestBody в случае его наличия в параметрах контроллера.
     *
     * @param body тело запроса.
     * @param inputMessage входящее http-сообщение.
     * @param parameter параметр метода контроллера, которому применена аннотация.
     * @param targetType тип, к которому должно быть преобразовано тело запроса.
     * @param converterType тип конвертера, который преобразует тело запроса в объект.
     * @return тело запроса.
     */

    @Override
    public Object afterBodyRead(@Nullable Object body,
                                @NonNull HttpInputMessage inputMessage,
                                @NonNull MethodParameter parameter,
                                @NonNull Type targetType,
                                @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            logger.info("Request body: %s".formatted(Objects.isNull(body) ? "{}" : mapper.writeValueAsString(body)));
        } catch (JsonProcessingException e) {
            logger.error("JsonProcessingException while parsing body: %s".formatted(e.getMessage()));
        }
        return body;
    }

    /**
     * Логирует предупреждение, если метод ожидает @RequestBody, но мы его не передаем.
     *
     * @param body тело запроса.
     * @param inputMessage входящее http-сообщение.
     * @param parameter параметр метода контроллера, которому применена аннотация.
     * @param targetType тип, к которому должно быть преобразовано тело запроса.
     * @param converterType тип конвертера, который преобразует тело запроса в объект.
     * @return тело запроса.
     */
    @Override
    public Object handleEmptyBody(@Nullable Object body,
                                  @NonNull HttpInputMessage inputMessage,
                                  @NonNull MethodParameter parameter,
                                  @NonNull Type targetType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        logger.warn("The empty request body was passed to a method that expects a non-empty body.");
        return body;
    }

}

