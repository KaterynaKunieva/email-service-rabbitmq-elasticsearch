package org.kunievakateryna.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for RabbitMQ messaging.
 * Defines exchanges, queues, and bindings for email notifications.
 */
@Configuration
public class RabbitConfig {

    /** Name of the fanout exchange for email notifications */
    public static final String EXCHANGE_EMAIL_NOTIFICATIONS = "email-notifications-exchange";

    @Value("${app.rabbitmq.email-queue}")
    private String emailQueueName;

    /**
     * Defines the durable queue for receiving email messages
     *
     * @return configured Queue object
     */
    @Bean
    public Queue emailQueue() {
        return new Queue(emailQueueName, true);
    }

    /**
     * Configures JSON message converter for RabbitMQ
     *
     * @return Jackson2JsonMessageConverter instance
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Defines the fanout exchange for broadcasting email notifications
     *
     * @return FanoutExchange instance
     */
    @Bean
    public FanoutExchange emailNotificationsExchange() {
        return new FanoutExchange(EXCHANGE_EMAIL_NOTIFICATIONS, true, false);
    }

    /**
     * Binds the email queue to the email notifications exchange
     *
     * @return Binding between the queue and exchange
     */
    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(emailNotificationsExchange());
    }
}