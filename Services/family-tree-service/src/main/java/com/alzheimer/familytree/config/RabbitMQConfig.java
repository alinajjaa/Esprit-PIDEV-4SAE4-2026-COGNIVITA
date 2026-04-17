package com.alzheimer.familytree.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE      = "family-tree.exchange";
    public static final String QUEUE         = "family-tree.risk-updates";
    public static final String ROUTING_KEY   = "risk.update";

    @Bean
    public TopicExchange familyTreeExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue riskUpdateQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding riskUpdateBinding(Queue riskUpdateQueue, TopicExchange familyTreeExchange) {
        return BindingBuilder.bind(riskUpdateQueue).to(familyTreeExchange).with(ROUTING_KEY);
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
