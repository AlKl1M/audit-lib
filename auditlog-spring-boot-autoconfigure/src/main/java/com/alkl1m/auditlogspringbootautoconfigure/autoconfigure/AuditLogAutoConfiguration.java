package com.alkl1m.auditlogspringbootautoconfigure.autoconfigure;

import com.alkl1m.auditlogspringbootautoconfigure.advice.HttpRequestLoggingAdvice;
import com.alkl1m.auditlogspringbootautoconfigure.advice.HttpResponseLoggingAdvice;
import com.alkl1m.auditlogspringbootautoconfigure.annotation.EnableHttpLogging;
import com.alkl1m.auditlogspringbootautoconfigure.appender.KafkaAppender;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(AuditLogProperties.class)
public class AuditLogAutoConfiguration implements WebMvcConfigurer {

    private final ApplicationContext applicationContext;
    private final AuditLogProperties properties;

    public AuditLogAutoConfiguration(ApplicationContext applicationContext, AuditLogProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
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

        if (properties.isKafkaLogEnabled()) {
            Appender kafkaAppender = config.getAppender("KafkaAppender");
            if (kafkaAppender == null) {
                kafkaAppender = createKafkaAppender(config);
            }
            packageLoggerConfig.addAppender(kafkaAppender, Level.INFO, null);
        }

        context.updateLoggers();
    }

    private Appender createKafkaAppender(org.apache.logging.log4j.core.config.Configuration config) {
        Property[] kafkaProperties = new Property[]{
                Property.createProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers()),
                Property.createProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer"),
                Property.createProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer"),
                Property.createProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true"),
                Property.createProperty(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "auditlog-id")
        };

        Appender kafkaAppender = KafkaAppender.createAppender(
                "KafkaAppender",
                null,
                null,
                properties.getTopic(),
                null,
                null,
                null,
                kafkaProperties
        );

        kafkaAppender.start();
        config.addAppender(kafkaAppender);
        return kafkaAppender;
    }

}

