package com.alzheimer.healthprevention.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Wellness risk → medical-records-service
    public static final String WELLNESS_EXCHANGE   = "health-prevention.exchange";
    public static final String WELLNESS_QUEUE      = "health-prevention.wellness-updates";
    public static final String WELLNESS_ROUTING_KEY = "wellness.update";

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
