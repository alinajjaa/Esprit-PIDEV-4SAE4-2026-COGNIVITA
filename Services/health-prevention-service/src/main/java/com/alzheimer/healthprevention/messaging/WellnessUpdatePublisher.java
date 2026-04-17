package com.alzheimer.healthprevention.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes wellness risk update events to RabbitMQ.
 * Replaces the unused BackendServiceClient Feign client:
 * the wellness score now reaches medical-records-service asynchronously.
 */
@Component
public class WellnessUpdatePublisher {

    private static final Logger log = LoggerFactory.getLogger(WellnessUpdatePublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public WellnessUpdatePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishWellnessUpdate(Long userId, Long medicalRecordId, double wellnessScore) {
        if (userId == null) return;
        try {
            WellnessUpdateEvent event = new WellnessUpdateEvent(userId, medicalRecordId, wellnessScore);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.WELLNESS_EXCHANGE,
                    RabbitMQConfig.WELLNESS_ROUTING_KEY,
                    event);
            log.info("[RabbitMQ] Published wellness update for userId={} score={}", userId, wellnessScore);
        } catch (Exception e) {
            log.warn("[RabbitMQ] Failed to publish wellness update for userId={}: {}", userId, e.getMessage());
        }
    }
}
