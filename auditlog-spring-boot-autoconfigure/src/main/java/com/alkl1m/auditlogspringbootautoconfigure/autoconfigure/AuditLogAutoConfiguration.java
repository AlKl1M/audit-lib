package com.alkl1m.auditlogspringbootautoconfigure.autoconfigure;

import com.alkl1m.auditlogspringbootautoconfigure.aspect.AuditLogAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс для AuditLog.
 *
 * @author alkl1m
 */
@Configuration
public class AuditLogAutoConfiguration {

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

}
