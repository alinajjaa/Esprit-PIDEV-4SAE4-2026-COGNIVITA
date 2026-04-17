package com.alzheimer.familytree.messaging;

import java.io.Serializable;

/**
 * Message published to RabbitMQ when a family tree change triggers
 * a hereditary risk recalculation for a user.
 */
public class RiskUpdateEvent implements Serializable {

    private Long   userId;
    private double hereditaryRiskScore;

    public RiskUpdateEvent() {}

    public RiskUpdateEvent(Long userId, double hereditaryRiskScore) {
        this.userId               = userId;
        this.hereditaryRiskScore  = hereditaryRiskScore;
    }

    public Long getUserId()                       { return userId; }
    public void setUserId(Long userId)            { this.userId = userId; }

    public double getHereditaryRiskScore()                          { return hereditaryRiskScore; }
    public void   setHereditaryRiskScore(double hereditaryRiskScore){ this.hereditaryRiskScore = hereditaryRiskScore; }

    @Override
    public String toString() {
        return "RiskUpdateEvent{userId=" + userId + ", score=" + hereditaryRiskScore + "}";
    }
}
