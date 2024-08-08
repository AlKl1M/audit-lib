package com.alkl1m.auditlogspringbootautoconfigure.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Хранилище конфигурационных свойств, связанных с аудитом логов.
 *
 * @author alkl1m
 */
@Data
@ConfigurationProperties(prefix = "audit-log")
public class AuditLogProperties {

    private boolean kafkaLogEnabled;
    private String bootstrapServers;
    private String topic;

}
