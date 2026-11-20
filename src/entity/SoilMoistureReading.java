package entity;

import java.time.LocalDateTime;

/** Entity: one soil-moisture value (percent) for a field, from a drone scan. */
public record SoilMoistureReading(int readingId, int fieldId, double moistureLevel, LocalDateTime timestamp) { }
