package com.alkl1m.auditloglistener.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Сущность для хранения информации о логах сервисов.
 *
 * @author alkl1m
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_source", nullable = false)
    private String serverSource;

    @Column(name = "method", nullable = false)
    private String method;

    @Column(name = "args", nullable = false)
    private String args;

    @Column(name = "result", nullable = false)
    private String result;

    @Column(name = "exception")
    private String exception;

}