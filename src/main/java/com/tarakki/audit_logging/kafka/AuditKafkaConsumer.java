package com.tarakki.audit_logging.kafka;

import com.tarakki.audit_logging.dto.AuditEventMessage;
import com.tarakki.audit_logging.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditKafkaConsumer {

    private final AuditLogService auditLogService;

    @KafkaListener(
            topics = "${audit.logging.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeAuditEvent(AuditEventMessage message) {
        log.info("Received Kafka audit log message: {}", message);
        try {
            auditLogService.save(message);
        } catch (Exception e) {
            log.error("Error processing received Kafka audit log message: {}", message, e);
            throw e;
        }
    }
}
