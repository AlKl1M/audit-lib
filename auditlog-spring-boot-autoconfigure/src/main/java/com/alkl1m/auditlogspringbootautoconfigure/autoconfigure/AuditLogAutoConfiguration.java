package com.alkl1m.auditlogspringbootautoconfigure.autoconfigure;

import com.alkl1m.auditlogspringbootautoconfigure.advice.HttpRequestLoggingAdvice;
import com.alkl1m.auditlogspringbootautoconfigure.advice.HttpResponseLoggingAdvice;
import com.alkl1m.auditlogspringbootautoconfigure.annotation.EnableHttpLogging;
import com.alkl1m.auditlogspringbootautoconfigure.appender.KafkaAppender;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.alkl1m.auditlogspringbootautoconfigure.aspect.AuditLogAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * Конфигурационный класс для AuditLog.
 *
 * @author alkl1m
 */
@Configuration
public class AuditLogAutoConfiguration implements WebMvcConfigurer {

    private final ApplicationContext applicationContext;

    public AuditLogAutoConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Создает бин AuditLogAspect в контексте приложения,
     * если в контексте отсутствует другой бин этого типа.
     *
     * @return экземпляр AuditLogAspect.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditLogAspect aspect() {
        return new AuditLogAspect();
    }

    /**
     * Создает бин HttpRequestLoggingAdvice в контексте приложения,
     * если в контексте отсутствует другой бин этого типа.
     *
     * @return экземпляр HttpRequestLoggingAdvice.
     */
    @Bean
    public HttpRequestLoggingAdvice httpRequestLoggingAdvice() {
        return applicationContext.getBeansWithAnnotation(EnableHttpLogging.class).isEmpty() ? null : new HttpRequestLoggingAdvice();
    }

    /**
     * Создает бин HttpResponseLoggingAdvice в контексте приложения,
     * если в контексте отсутствует другой бин этого типа.
     *
     * @return экземпляр HttpResponseLoggingAdvice.
     */
    @Bean
    public HttpResponseLoggingAdvice httpResponseLoggingAdvice() {
        return applicationContext.getBeansWithAnnotation(EnableHttpLogging.class).isEmpty() ? null : new HttpResponseLoggingAdvice();
    }

    @PostConstruct
    public void configureLogger() {
        configureAppender();
    }

    private void configureAppender() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();

        LoggerConfig packageLoggerConfig = config.getLoggerConfig("com.alkl1m.auditlogspringbootautoconfigure.aspect");

        Appender kafkaAppender = config.getAppender("KafkaAppender");
        if (kafkaAppender == null) {
            kafkaAppender = KafkaAppender
                    .createAppender(
                            "KafkaAppender",
                            null,
                            "true",
                            "send-auditlog-event",
                            "true",
                            "false",
                            null,
                            new Property[]{}
                    );
            kafkaAppender.start();
            config.addAppender(kafkaAppender);
        }

        packageLoggerConfig.addAppender(kafkaAppender, Level.INFO, null);

        context.updateLoggers();
    }

}

