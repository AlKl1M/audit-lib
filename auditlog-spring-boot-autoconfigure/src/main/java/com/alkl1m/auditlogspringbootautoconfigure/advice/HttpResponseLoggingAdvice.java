package com.alkl1m.auditlogspringbootautoconfigure.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Objects;

/**
 * Advice для логирования HTTP-ответов.
 *
 * @author alkl1m
 */
@ControllerAdvice
public class HttpResponseLoggingAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger logger = LogManager.getLogger(HttpResponseLoggingAdvice.class);


    /**
     * Проверка поддержки метода.
     *
     * @param returnType    тип возвращаемого значения метода.
     * @param converterType тип конвертера сообщений.
     * @return всегда true - advice применим ко всем типам возвращаемых значений.
     */
    @Override
    public boolean supports(@NonNull MethodParameter returnType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /**
     * Отлавливает после отработки метода контроллера request и response
     * и логирует сперва данные про request (метод и uri), а затем
     * логирует данные response.
     *
     * @param body                  тело ответа.
     * @param returnType            тип возвращаемого значения метода.
     * @param selectedContentType   выбранный тип контента.
     * @param selectedConverterType выбранный тип конвертера.
     * @param request               объект запроса.
     * @param response              объект ответа.
     * @return тело ответа.
     */
    @Override
    public Object beforeBodyWrite(@Nullable Object body,
                                  @NonNull MethodParameter returnType,
                                  @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request,
                                  @NonNull ServerHttpResponse response) {
        ObjectMapper mapper = new ObjectMapper();
        HttpServletResponse httpServletResponse = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        if (httpServletResponse != null) {
            try {
                String responseBody = Objects.isNull(body) ? "{}" : mapper.writeValueAsString(body);
                logger.info("Request method: %s request url: %s response status: %s response body: %s"
                        .formatted(request.getMethod(),
                                request.getURI(),
                                httpServletResponse.getStatus(),
                                responseBody));
            } catch (JsonProcessingException e) {
                logger.error("JsonProcessingException while parsing body: %s".formatted(e.getMessage()));
            }
        }
        return body;
    }

}
