package com.tarakki.audit_logging.kafka;

import com.tarakki.audit_logging.datafactory.AuditEventMessageDataFactory;
import com.tarakki.audit_logging.dto.AuditEventMessage;
import com.tarakki.audit_logging.service.AuditLogService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditKafkaConsumerTest {

    @Test
    void shouldCallServiceWhenMessageReceived() {
        AuditLogService auditLogService = mock(AuditLogService.class);
        AuditKafkaConsumer consumer = new AuditKafkaConsumer(auditLogService);
        AuditEventMessage event = AuditEventMessageDataFactory.defaultEvent();

        consumer.consumeAuditEvent(event);

        verify(auditLogService).save(event);
    }

    @Test
    void shouldPropagateExceptionWhenServiceFails() {
        AuditLogService auditLogService = mock(AuditLogService.class);
        AuditKafkaConsumer consumer = new AuditKafkaConsumer(auditLogService);
        AuditEventMessage event = AuditEventMessageDataFactory.defaultEvent();
        RuntimeException failure = new RuntimeException("Database error");
        doThrow(failure).when(auditLogService).save(event);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> consumer.consumeAuditEvent(event));

        verify(auditLogService).save(event);
        org.junit.jupiter.api.Assertions.assertSame(failure, thrown);
    }
}
