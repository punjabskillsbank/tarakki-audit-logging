package com.tarakki.audit_logging.serviceImpl;

import com.tarakki.audit_logging.datafactory.AuditEventMessageDataFactory;
import com.tarakki.audit_logging.dto.AuditEventMessage;
import com.tarakki.audit_logging.entity.AuditLog;
import com.tarakki.audit_logging.enums.BoardTaskAuditEventType;
import com.tarakki.audit_logging.exception.InvalidAuditEventException;
import com.tarakki.audit_logging.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditLogServiceImplTest {

    private AuditLogRepository auditLogRepository;
    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        auditLogService = new AuditLogServiceImpl(auditLogRepository, new ObjectMapper());
    }

    @Test
    void shouldMapAndSaveAuditEventSuccessfully() {
        auditLogService.save(AuditEventMessageDataFactory.defaultEvent());

        AuditLog savedLog = capturedAuditLog();
        assertEquals(AuditEventMessageDataFactory.SERVICE_NAME, savedLog.getServiceName());
        assertEquals(AuditEventMessageDataFactory.ENTITY_NAME, savedLog.getEntityName());
        assertEquals(AuditEventMessageDataFactory.ENTITY_ID, savedLog.getEntityId());
        assertEquals(BoardTaskAuditEventType.BOARD_DELETED, savedLog.getEventName());
        assertEquals(AuditEventMessageDataFactory.SYSTEM_USER_ID, savedLog.getPerformedBy());
        assertEquals(AuditEventMessageDataFactory.OLD_VALUE_JSON, savedLog.getOldValue());
        assertNull(savedLog.getNewValue());
        assertEquals(AuditEventMessageDataFactory.EVENT_TIME, savedLog.getEventTime());
    }

    @Test
    void shouldMapAllSupportedEventNameAliasesIgnoringCase() {
        Map<String, BoardTaskAuditEventType> eventNames = Map.of(
                AuditEventMessageDataFactory.DELETE_BOARD_EVENT, BoardTaskAuditEventType.BOARD_DELETED,
                AuditEventMessageDataFactory.BOARD_DELETED_EVENT, BoardTaskAuditEventType.BOARD_DELETED,
                AuditEventMessageDataFactory.UPDATE_BOARD_EVENT, BoardTaskAuditEventType.BOARD_UPDATED,
                AuditEventMessageDataFactory.BOARD_UPDATED_EVENT, BoardTaskAuditEventType.BOARD_UPDATED);

        eventNames.forEach((eventName, expectedEventType) -> {
            auditLogService.save(AuditEventMessageDataFactory.event(eventName,
                    AuditEventMessageDataFactory.SYSTEM_USER, null, null,
                    AuditEventMessageDataFactory.EVENT_TIME.toString()));
            assertEquals(expectedEventType, capturedAuditLog().getEventName());
        });
    }

    @Test
    void shouldRejectNullAndUnsupportedEventNames() {
        assertThrows(InvalidAuditEventException.class, () -> auditLogService.save(
                AuditEventMessageDataFactory.event(null, AuditEventMessageDataFactory.SYSTEM_USER, null, null, null)));
        assertThrows(InvalidAuditEventException.class, () -> auditLogService.save(
                AuditEventMessageDataFactory.event(AuditEventMessageDataFactory.UNSUPPORTED_EVENT,
                        AuditEventMessageDataFactory.SYSTEM_USER, null, null, null)));
    }

    @Test
    void shouldUseSystemUserForNullOrInvalidPerformedBy() {
        for (String performedBy : List.of(AuditEventMessageDataFactory.SYSTEM_USER,
                AuditEventMessageDataFactory.INVALID_USER_ID)) {
            auditLogService.save(AuditEventMessageDataFactory.event(AuditEventMessageDataFactory.DELETE_BOARD_EVENT,
                    performedBy, null, null, null));
            assertEquals(AuditEventMessageDataFactory.SYSTEM_USER_ID, capturedAuditLog().getPerformedBy());
        }

        auditLogService.save(AuditEventMessageDataFactory.event(AuditEventMessageDataFactory.DELETE_BOARD_EVENT,
                null, null, null, null));
        assertEquals(AuditEventMessageDataFactory.SYSTEM_USER_ID, capturedAuditLog().getPerformedBy());
    }

    @Test
    void shouldMapUserAndJsonPayloads() throws Exception {
        auditLogService.save(AuditEventMessageDataFactory.updateEvent());

        AuditLog savedLog = capturedAuditLog();
        assertEquals(AuditEventMessageDataFactory.USER_ID, savedLog.getPerformedBy());
        assertEquals(BoardTaskAuditEventType.BOARD_UPDATED, savedLog.getEventName());
        assertEquals(AuditEventMessageDataFactory.newValueMap(),
                new ObjectMapper().readValue(savedLog.getNewValue(), Map.class));
    }

    @Test
    void shouldSerializePlainStringsAndPreserveJsonArrays() {
        auditLogService.save(AuditEventMessageDataFactory.event(AuditEventMessageDataFactory.DELETE_BOARD_EVENT,
                AuditEventMessageDataFactory.SYSTEM_USER,
                AuditEventMessageDataFactory.PLAIN_VALUE, AuditEventMessageDataFactory.JSON_ARRAY,
                AuditEventMessageDataFactory.EVENT_TIME.toString()));

        AuditLog savedLog = capturedAuditLog();
        assertEquals(AuditEventMessageDataFactory.SERIALIZED_PLAIN_VALUE, savedLog.getOldValue());
        assertEquals(AuditEventMessageDataFactory.JSON_ARRAY, savedLog.getNewValue());
    }

    @Test
    void shouldUseCurrentTimeForMissingOrInvalidEventTime() {
        Instant before = Instant.now();
        auditLogService.save(AuditEventMessageDataFactory.event(AuditEventMessageDataFactory.DELETE_BOARD_EVENT,
                AuditEventMessageDataFactory.SYSTEM_USER,
                null, null, null));
        assertTrue(!capturedAuditLog().getEventTime().isBefore(before));

        auditLogService.save(AuditEventMessageDataFactory.event(AuditEventMessageDataFactory.DELETE_BOARD_EVENT,
                AuditEventMessageDataFactory.SYSTEM_USER,
                null, null, AuditEventMessageDataFactory.INVALID_EVENT_TIME));
        assertTrue(!capturedAuditLog().getEventTime().isBefore(before));
    }

    @Test
    void shouldPropagateRepositoryFailures() {
        RuntimeException failure = new RuntimeException("Repository failure");
        doThrow(failure).when(auditLogRepository).save(org.mockito.ArgumentMatchers.any(AuditLog.class));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> auditLogService.save(AuditEventMessageDataFactory.defaultEvent()));

        assertEquals(failure, thrown);
    }

    private AuditLog capturedAuditLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, atLeastOnce()).save(captor.capture());
        List<AuditLog> auditLogs = captor.getAllValues();
        return auditLogs.get(auditLogs.size() - 1);
    }
}
