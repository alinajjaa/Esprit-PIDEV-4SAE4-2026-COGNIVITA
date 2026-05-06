package com.cognivita.dashboardservice.dto;

public class AlertTrendDto {

    private final String date;
    private final Long totalAlerts;
    private final Long outOfZoneCount;
    private final Long noMovementCount;

    public AlertTrendDto(Object date,
                         Number totalAlerts,
                         Number outOfZoneCount,
                         Number noMovementCount) {
        this.date = date == null ? null : date.toString();
        this.totalAlerts = toLong(totalAlerts);
        this.outOfZoneCount = toLong(outOfZoneCount);
        this.noMovementCount = toLong(noMovementCount);
    }

    public String getDate() {
        return date;
    }

    public Long getTotalAlerts() {
        return totalAlerts;
    }

    public Long getOutOfZoneCount() {
        return outOfZoneCount;
    }

    public Long getNoMovementCount() {
        return noMovementCount;
    }

    private Long toLong(Number value) {
        return value == null ? 0L : value.longValue();
    }
}
