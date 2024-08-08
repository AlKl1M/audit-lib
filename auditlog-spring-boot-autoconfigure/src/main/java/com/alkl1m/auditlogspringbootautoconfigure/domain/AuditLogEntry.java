package com.alkl1m.auditlogspringbootautoconfigure.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogEntry implements Serializable {
    private String serverSource;
    private String method;
    private Object[] args;
    private Object result;
    private String exception;
}