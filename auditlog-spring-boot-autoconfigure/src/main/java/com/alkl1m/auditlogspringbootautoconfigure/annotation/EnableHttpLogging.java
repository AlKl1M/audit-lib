package com.alkl1m.auditlogspringbootautoconfigure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для включения в конфигурацию бинов.
 * HttpRequestLoggingAdvice и HttpResponseLoggingAdvice,
 * которые записывают все логи в файл.
 *
 * @author alkl1m
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnableHttpLogging {
}
