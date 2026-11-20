package entity;

import java.time.LocalDateTime;

/** Entity: a drone mission. fieldId 0 means the mission covers all fields. */
public record Mission(int missionId, LocalDateTime scheduledTime, String status, int flightPathId,
                      String missionType, int fieldId, String droneId) { }
