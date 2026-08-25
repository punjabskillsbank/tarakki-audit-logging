package com.tarakki.audit_logging.serviceImpl;

import com.tarakki.audit_logging.dto.AuditEventMessage;
import com.tarakki.audit_logging.entity.AuditLog;
import com.tarakki.audit_logging.exception.InvalidAuditEventException;
import com.tarakki.audit_logging.repository.AuditLogRepository;
import com.tarakki.audit_logging.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private static final UUID SYSTEM_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void save(AuditEventMessage event) {
        log.info("Mapping and persisting audit log: service={}, entity={}, action={}, by={}",
                event.serviceName(), event.entityName(), event.eventName(), event.performedBy());

        try {
            AuditLog auditLog = AuditLog.builder()
                    .serviceName(event.serviceName())
                    .entityName(event.entityName())
                    .entityId(event.entityId())
                    .eventName(mapEventName(event.eventName()))
                    .performedBy(mapPerformedBy(event.performedBy()))
                    .oldValue(mapJsonField(event.oldValue()))
                    .newValue(mapJsonField(event.newValue()))
                    .eventTime(mapEventTime(event.eventTime()))
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Successfully persisted audit log for entity_id={}", event.entityId());
        } catch (Exception e) {
            log.error("Failed to persist audit log event", e);
            throw e;
        }
    }

    private String mapEventName(String eventName) {
        if (eventName == null) {
            throw new InvalidAuditEventException();
        }
        return eventName;
    }

    private UUID mapPerformedBy(String performedBy) {
        if (performedBy == null || "SYSTEM".equalsIgnoreCase(performedBy)) {
            return SYSTEM_UUID;
        }
        try {
            return UUID.fromString(performedBy);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for performed_by: '{}', falling back to system UUID", performedBy);
            return SYSTEM_UUID;
        }
    }

    private String mapJsonField(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof String str) {
            if (str.trim().startsWith("{") || str.trim().startsWith("[")) {
                return str;
            } else {
                try {
                    return objectMapper.writeValueAsString(str);
                } catch (Exception e) {
                    log.error("Failed to serialize string to JSON", e);
                    return null;
                }
            }
        }
        try {
            return objectMapper.writeValueAsString(val);
        } catch (Exception e) {
            log.error("Failed to serialize JSON field", e);
            return null;
        }
    }

    private Instant mapEventTime(String eventTime) {
        if (eventTime == null) {
            return Instant.now();
        }
        try {
            return Instant.parse(eventTime);
        } catch (Exception e) {
            log.warn("Failed to parse event time '{}', using current time", eventTime);
            return Instant.now();
        }
    }
}
