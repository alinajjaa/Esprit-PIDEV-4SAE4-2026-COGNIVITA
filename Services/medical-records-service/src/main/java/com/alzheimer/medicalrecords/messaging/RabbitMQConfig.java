package com.alzheimer.medicalrecords.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares all queues this service listens to:
 *   1. family-tree.risk-updates   — from family-tree-service (hereditary risk)
 *   2. health-prevention.wellness-updates — from health-prevention-service (wellness risk)
 */
@Configuration
public class RabbitMQConfig {

    // ── Family-tree exchange & queue ──────────────────────────────────────────
    public static final String FAMILY_EXCHANGE    = "family-tree.exchange";
    public static final String FAMILY_QUEUE       = "family-tree.risk-updates";
    public static final String FAMILY_ROUTING_KEY = "risk.update";

    // ── Health-prevention exchange & queue ────────────────────────────────────
    public static final String WELLNESS_EXCHANGE    = "health-prevention.exchange";
    public static final String WELLNESS_QUEUE       = "health-prevention.wellness-updates";
    public static final String WELLNESS_ROUTING_KEY = "wellness.update";

    // ── Family-tree beans ─────────────────────────────────────────────────────
    @Bean
    public TopicExchange familyTreeExchange() {
        return ExchangeBuilder.topicExchange(FAMILY_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue riskUpdateQueue() {
        return QueueBuilder.durable(FAMILY_QUEUE).build();
    }

    @Bean
    public Binding riskUpdateBinding(Queue riskUpdateQueue, TopicExchange familyTreeExchange) {
        return BindingBuilder.bind(riskUpdateQueue).to(familyTreeExchange).with(FAMILY_ROUTING_KEY);
    }

    // ── Health-prevention beans ───────────────────────────────────────────────
    @Bean
    public TopicExchange healthPreventionExchange() {
        return ExchangeBuilder.topicExchange(WELLNESS_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue wellnessUpdateQueue() {
        return QueueBuilder.durable(WELLNESS_QUEUE).build();
    }

    @Bean
    public Binding wellnessUpdateBinding(Queue wellnessUpdateQueue,
                                         TopicExchange healthPreventionExchange) {
        return BindingBuilder.bind(wellnessUpdateQueue)
                .to(healthPreventionExchange)
                .with(WELLNESS_ROUTING_KEY);
    }

    // ── Shared infrastructure ─────────────────────────────────────────────────
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
