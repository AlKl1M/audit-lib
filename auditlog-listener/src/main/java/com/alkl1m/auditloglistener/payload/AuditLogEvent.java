package com.alkl1m.auditloglistener.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Event который приходит из кафки для отображения логов сервисов.
 *
 * @author alkl1m
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogEvent implements Serializable {

    private String serverSource;
    private String method;
    private String[] args;
    private String result;
    private String exception;

}