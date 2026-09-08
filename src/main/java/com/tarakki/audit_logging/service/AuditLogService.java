package com.tarakki.audit_logging.service;

import com.tarakki.audit_logging.dto.AuditEventMessage;

public interface AuditLogService {
    void save(AuditEventMessage event);
}
