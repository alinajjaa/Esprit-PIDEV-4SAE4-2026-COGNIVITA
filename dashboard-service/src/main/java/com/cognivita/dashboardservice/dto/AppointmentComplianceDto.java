package com.cognivita.dashboardservice.dto;

import com.cognivita.dashboardservice.enums.AppointmentType;
import com.cognivita.dashboardservice.enums.RiskLevel;

public class AppointmentComplianceDto {

    private final RiskLevel riskLevel;
    private final AppointmentType appointmentType;
    private final Long totalAppointments;
    private final Long completedCount;
    private final Long missedCount;
    private final Long cancelledCount;
    private final Double avgDelayHours;

    public AppointmentComplianceDto(RiskLevel riskLevel,
                                    AppointmentType appointmentType,
                                    Long totalAppointments,
                                    Long completedCount,
                                    Long missedCount,
                                    Long cancelledCount,
                                    Double avgDelayHours) {
        this.riskLevel = riskLevel;
        this.appointmentType = appointmentType;
        this.totalAppointments = totalAppointments;
        this.completedCount = completedCount;
        this.missedCount = missedCount;
        this.cancelledCount = cancelledCount;
        this.avgDelayHours = avgDelayHours;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public AppointmentType getAppointmentType() {
        return appointmentType;
    }

    public Long getTotalAppointments() {
        return totalAppointments;
    }

    public Long getCompletedCount() {
        return completedCount;
    }

    public Long getMissedCount() {
        return missedCount;
    }

    public Long getCancelledCount() {
        return cancelledCount;
    }

    public Double getAvgDelayHours() {
        return avgDelayHours;
    }
}
