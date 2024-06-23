package com.alkl1m.auditlogspringbootautoconfigure.autoconfigure;

import com.alkl1m.auditlogspringbootautoconfigure.advice.HttpRequestLoggingAdvice;
import com.alkl1m.auditlogspringbootautoconfigure.advice.HttpResponseLoggingAdvice;
import com.alkl1m.auditlogspringbootautoconfigure.annotation.EnableHttpLogging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурационный класс для AuditLog.
 *
 * @author alkl1m
 */
@Configuration
public class AuditLogAutoConfiguration implements WebMvcConfigurer {

    @Autowired
    private ApplicationContext applicationContext;

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

}
