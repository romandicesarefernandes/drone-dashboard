package entity;

import java.time.LocalDateTime;

/** Entity: an alert raised when a monitored value crosses a threshold. */
public record Alert(int alertId, int fieldId, String severity, String message, String status, LocalDateTime raisedAt) { }
