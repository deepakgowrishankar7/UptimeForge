package com.pulsewatch.audit;

import com.pulsewatch.entity.AuditLog;
import com.pulsewatch.entity.User;
import com.pulsewatch.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(User actor, String action, String resourceType, String resourceId, String details, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog(actor, action, resourceType, resourceId, details, ipAddress);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to write audit log for action: {} on resource: {}", action, resourceType, e);
        }
    }
}
