package com.alkl1m.auditlogspringbootautoconfigure.domain;

import java.io.Serializable;

public class AuditLogEntry implements Serializable {
    private String serverSource;
    private String method;
    private Object[] args;
    private Object result;
    private String exception;

    public AuditLogEntry(String serverSource, String method, Object[] args, Object result, String exception) {
        this.serverSource = serverSource;
        this.method = method;
        this.args = args;
        this.result = result;
        this.exception = exception;
    }

    public String getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public Object getResult() {
        return result;
    }

    public String getException() {
        return exception;
    }

    public String getServerSource() {
        return serverSource;
    }

    public void setServerSource(String serverSource) {
        this.serverSource = serverSource;
    }
}