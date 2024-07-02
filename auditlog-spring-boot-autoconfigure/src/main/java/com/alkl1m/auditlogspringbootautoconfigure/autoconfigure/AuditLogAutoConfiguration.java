package com.alkl1m.auditlogspringbootautoconfigure.autoconfigure;

import com.alkl1m.auditlogspringbootautoconfigure.advice.HttpRequestLoggingAdvice;
import com.alkl1m.auditlogspringbootautoconfigure.advice.HttpResponseLoggingAdvice;
import com.alkl1m.auditlogspringbootautoconfigure.annotation.EnableHttpLogging;
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

