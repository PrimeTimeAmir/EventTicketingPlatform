package kz.kimep.ticketing.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EVENT_EXCHANGE = "ticketing.events";
    public static final String EVENT_QUEUE = "ticketing.events.queue";
    public static final String EVENT_ROUTING_KEY = "ticketing.event";

    public static final String DEAD_LETTER_EXCHANGE =
            "ticketing.events.dlx";

    public static final String DEAD_LETTER_QUEUE =
            "ticketing.events.dlq";

    public static final String DEAD_LETTER_ROUTING_KEY =
            "ticketing.event.dead";

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE);
    }

    @Bean
    public Queue eventQueue() {
        return QueueBuilder.durable(EVENT_QUEUE)
                .withArgument(
                        "x-dead-letter-exchange",
                        DEAD_LETTER_EXCHANGE
                )
                .withArgument(
                        "x-dead-letter-routing-key",
                        DEAD_LETTER_ROUTING_KEY
                )
                .build();
    }

    @Bean
    public Binding eventBinding() {
        return BindingBuilder
                .bind(eventQueue())
                .to(eventExchange())
                .with(EVENT_ROUTING_KEY);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DEAD_LETTER_ROUTING_KEY);
    }
}
