package com.alzheimer.medicalrecords.messaging;

import java.io.Serializable;

/**
 * Event received from family-tree-service via RabbitMQ
 * whenever a family member is added, updated, or deleted.
 */
public class RiskUpdateEvent implements Serializable {

    private Long   userId;
    private double hereditaryRiskScore;

    public RiskUpdateEvent() {}

    public RiskUpdateEvent(Long userId, double hereditaryRiskScore) {
        this.userId              = userId;
        this.hereditaryRiskScore = hereditaryRiskScore;
    }

    public Long   getUserId()                                        { return userId; }
    public void   setUserId(Long userId)                             { this.userId = userId; }
    public double getHereditaryRiskScore()                           { return hereditaryRiskScore; }
    public void   setHereditaryRiskScore(double hereditaryRiskScore) { this.hereditaryRiskScore = hereditaryRiskScore; }
}
