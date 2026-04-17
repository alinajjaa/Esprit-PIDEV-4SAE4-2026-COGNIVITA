package com.cognivita.dashboardservice.dto;

import com.cognivita.dashboardservice.enums.AlertType;

public class AlertTypePressureDto {

    private final AlertType type;
    private final Long totalCount;
    private final Long unreadCount;
    private final Long weightedSeverity;
    private final Double avgAgeHours;

    public AlertTypePressureDto(AlertType type,
                                Long totalCount,
                                Long unreadCount,
                                Long weightedSeverity,
                                Double avgAgeHours) {
        this.type = type;
        this.totalCount = totalCount;
        this.unreadCount = unreadCount;
        this.weightedSeverity = weightedSeverity;
        this.avgAgeHours = avgAgeHours;
    }

    public AlertType getType() {
        return type;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public Long getUnreadCount() {
        return unreadCount;
    }

    public Long getWeightedSeverity() {
        return weightedSeverity;
    }

    public Double getAvgAgeHours() {
        return avgAgeHours;
    }
}
