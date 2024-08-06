package com.alkl1m.auditlogspringbootautoconfigure.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "audit-log")
public class AuditLogProperties {

    private boolean kafkaLogEnabled;
    private String bootstrapServers;
    private String topic;
}
