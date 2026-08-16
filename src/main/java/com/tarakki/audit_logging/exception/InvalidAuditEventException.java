package com.tarakki.audit_logging.exception;

/**
 * Thrown when an audit event contains a missing or unsupported event name.
 */
public class InvalidAuditEventException extends RuntimeException {

    public InvalidAuditEventException(String message) {
        super(message);
    }
}
