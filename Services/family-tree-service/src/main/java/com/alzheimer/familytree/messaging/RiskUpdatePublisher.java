package com.alzheimer.familytree.messaging;

import com.alzheimer.familytree.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes hereditary risk update events to RabbitMQ.
 * Replaces the old blocking synchronous HTTP call to medical-records-service.
 */
@Component
public class RiskUpdatePublisher {

    private static final Logger log = LoggerFactory.getLogger(RiskUpdatePublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RiskUpdatePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes a fire-and-forget risk update event.
     * The family-tree endpoint returns immediately; medical-records-service
     * processes the message asynchronously.
     */
    public void publishRiskUpdate(Long userId, double score) {
        if (userId == null) return;
        try {
            RiskUpdateEvent event = new RiskUpdateEvent(userId, score);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    event);
            log.info("[RabbitMQ] Published risk update for userId={} score={}", userId, score);
        } catch (Exception e) {
            // Non-fatal: log and continue. The CRUD operation already succeeded.
            log.warn("[RabbitMQ] Failed to publish risk update for userId={}: {}", userId, e.getMessage());
        }
    }
}
