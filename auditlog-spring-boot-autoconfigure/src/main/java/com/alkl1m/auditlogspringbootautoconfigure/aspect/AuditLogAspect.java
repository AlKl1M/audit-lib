package com.alkl1m.auditlogspringbootautoconfigure.aspect;

import com.alkl1m.auditlogspringbootautoconfigure.annotation.AuditLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.logging.LogLevel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Данный аспект предоставляет возможность логирования
 * вокруг точек среза, помеченных аннотацией @AuditLog.
 *
 * @author alkl1m
 */
@Aspect
@Component
public class AuditLogAspect {

    private Logger logger = LogManager.getLogger(AuditLogAspect.class);

    /**
     * Определяет точку среза для методов, помеченных @AuditLog.
     *
     * @param auditLog аннотация для пометки методов, подлежащих логированию.
     */
    @Pointcut("@annotation(auditLog)")
    public void auditLogPointcut(AuditLog auditLog) {}

    /**
     * Метод, выполняющий логирование аудита вокруг метода, помеченного аннотацией @AuditLog.
     * Получает информацию о точке соединения и самой аннотации, обрабатывает метод,
     * записывает лог.
     *
     * @param joinPoint объект, представляющий точку соединения.
     * @param auditLog аннотация, содержащая информацию о логировании.
     */
    @Around(value = "auditLogPointcut(auditLog)", argNames = "joinPoint,auditLog")
    public void logMethodData(ProceedingJoinPoint joinPoint, AuditLog auditLog) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        try {
            Object result = joinPoint.proceed();
            String logMessage = "Method: " + methodName + ", Args: " + argsToString(args) + ", Result: " + (result != null ? result.toString() : "void");
            logMessage(logMessage, auditLog.logLevel());
        } catch (Throwable e) {
            String errorMessage = "Exception in method: " + methodName + ", Exception: " + e.toString();
            logMessage(errorMessage, LogLevel.ERROR);
        }
    }

    /**
     * Метод для логирования сообщений с заданным уровнем.
     *
     * @param message сообщение для логирования.
     * @param logLevel уровень логирования (DEBUG, ERROR, WARN, TRACE)
     */
    private void logMessage(String message, LogLevel logLevel) {
        switch (logLevel) {
            case DEBUG -> logger.debug(message);
            case ERROR -> logger.error(message);
            case WARN -> logger.warn(message);
            case TRACE -> logger.trace(message);
            default -> logger.info(message);
        }
    }

    /**
     * Метод для преобразования массива аргументов в строку.
     *
     * @param args массив аргументов.
     * @return строка, содержащая значения аргументов через запятую.
     */
    private String argsToString(Object[] args) {
        return Arrays.stream(args)
                .map(arg -> (arg != null) ? arg.toString() : "null")
                .collect(Collectors.joining(", "));
    }

}
