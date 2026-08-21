package com.tarakki.audit_logging.datafactory;

import com.tarakki.audit_logging.dto.AuditEventMessage;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class AuditEventMessageDataFactory {

    public static final String SERVICE_NAME = "board-task-service";
    public static final String ENTITY_NAME = "BOARD";
    public static final String ENTITY_ID = "123";
    public static final String SYSTEM_USER = "SYSTEM";
    public static final String DELETE_BOARD_EVENT = "DELETE_BOARD";
    public static final String BOARD_DELETED_EVENT = "board_deleted";
    public static final String UPDATE_BOARD_EVENT = "UPDATE_BOARD";
    public static final String BOARD_UPDATED_EVENT = "board_updated";
    public static final UUID USER_ID = UUID.fromString("43b4da6a-6d01-4e3f-b71d-b9b9e4c51a58");
    public static final UUID SYSTEM_USER_ID = new UUID(0L, 0L);
    public static final Instant EVENT_TIME = Instant.parse("2026-08-10T12:00:00Z");
    public static final String OLD_VALUE_JSON = "{\"boardName\":\"Test Board\",\"boardDesc\":\"Desc\"}";
    public static final String UNSUPPORTED_EVENT = "UNSUPPORTED";
    public static final String INVALID_USER_ID = "not-a-uuid";
    public static final String INVALID_EVENT_TIME = "invalid-time";
    public static final String PLAIN_VALUE = "plain value";
    public static final String JSON_ARRAY = "[1,2]";
    public static final String SERIALIZED_PLAIN_VALUE = "\"plain value\"";
    public static final String KAFKA_TOPIC = "audit-logging-test";
    public static final String KAFKA_CONSUMER_GROUP = "audit-log-test-consumer-group";

    private AuditEventMessageDataFactory() {
    }

    public static AuditEventMessage defaultEvent() {
        return event(DELETE_BOARD_EVENT, SYSTEM_USER, OLD_VALUE_JSON, null, EVENT_TIME.toString());
    }

    public static AuditEventMessage event(String eventName, String performedBy, Object oldValue, Object newValue,
                                          String eventTime) {
        return new AuditEventMessage(SERVICE_NAME, ENTITY_NAME, ENTITY_ID, eventName, performedBy, oldValue, newValue,
                eventTime);
    }

    public static AuditEventMessage updateEvent() {
        return event(UPDATE_BOARD_EVENT, USER_ID.toString(), null, newValueMap(),
                EVENT_TIME.toString());
    }

    public static Map<String, Object> newValueMap() {
        return Map.of("boardName", "New Name", "version", 2);
    }
}
