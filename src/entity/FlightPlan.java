package entity;

import java.time.LocalDateTime;
import java.util.List;

/** A generated drone route. A plan can be scheduled more than once. */
public record FlightPlan(
        int flightPathId,
        String name,
        String missionType,
        List<Integer> fieldIds,
        String droneId,
        List<RoutePoint> waypoints,
        int estimatedMinutes,
        double batteryUsagePercent,
        double coverageAcres,
        String status,
        LocalDateTime createdAt) {

    public FlightPlan {
        fieldIds = List.copyOf(fieldIds);
        waypoints = List.copyOf(waypoints);
    }

    /** Coordinates use the same 1000 x 600 farm-map coordinate system as FieldBoundary. */
    public record RoutePoint(double x, double y) { }
}
