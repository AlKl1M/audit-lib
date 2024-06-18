package com.alkl1m.auditlogspringbootautoconfigure.annotation;

import org.springframework.boot.logging.LogLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для логгирования метода в соответствии
 * с указанным уровнем
 * @author alkl1m
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditLog {

    LogLevel logLevel() default LogLevel.INFO;

}
