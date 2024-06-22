package com.alkl1m.auditlogspringbootautoconfigure.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Advice для логирования HTTP-запросов.
 *
 * @author alkl1m
 */
@ControllerAdvice
public class HttpRequestLoggingAdvice implements RequestBodyAdvice {

    private Logger logger = LogManager.getLogger(HttpRequestLoggingAdvice.class);

    /**
     * Проверка поддержки метода.
     *
     * @param methodParameter параметр метода.
     * @param targetType тип, к которому должно быть преобразовано тело запроса.
     * @param converterType тип конвертера, который преобразует тело запроса в объект.
     * @return true, если метод поддерживается; иначе false.
     */
    @Override
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
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
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage,
                                           MethodParameter parameter,
                                           Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
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
    public Object afterBodyRead(Object body,
                                HttpInputMessage inputMessage,
                                MethodParameter parameter,
                                Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            logger.info("Request body: " + mapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            logger.error("JsonProcesingException while parsing body: " + e.getMessage());
        }
        return body;
    }

    /**
     * Логирует пустой JSON, если метод ожидает @RequestBody, но мы его не передаем.
     *
     * @param body тело запроса.
     * @param inputMessage входящее http-сообщение.
     * @param parameter параметр метода контроллера, которому применена аннотация.
     * @param targetType тип, к которому должно быть преобразовано тело запроса.
     * @param converterType тип конвертера, который преобразует тело запроса в объект.
     * @return тело запроса.
     */
    @Override
    public Object handleEmptyBody(Object body,
                                  HttpInputMessage inputMessage,
                                  MethodParameter parameter,
                                  Type targetType,
                                  Class<? extends HttpMessageConverter<?>> converterType) {
        logger.info("Request body: {}");
        return body;
    }

}

