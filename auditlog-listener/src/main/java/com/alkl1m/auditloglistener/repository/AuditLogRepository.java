package com.alkl1m.auditloglistener.repository;

import com.alkl1m.auditloglistener.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author alkl1m
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
