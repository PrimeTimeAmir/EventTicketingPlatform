package kz.kimep.ticketing.messaging.producer;

import kz.kimep.ticketing.messaging.config.RabbitMQConfig;
import kz.kimep.ticketing.messaging.domain.OutboxMessage;
import kz.kimep.ticketing.messaging.repo.OutboxMessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
class   OutboxPublisherIntegrationTest {

    @Container
    private static final RabbitMQContainer rabbitMQ =
            new RabbitMQContainer("rabbitmq:3-management-alpine")
                    .withAdminUser("ticketing")
                    .withAdminPassword("ticketing_secret");

    private CachingConnectionFactory connectionFactory;
    private RabbitTemplate rabbitTemplate;

    private OutboxMessageRepository outboxMessageRepository;
    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {

        // Connect to the RabbitMQ Testcontainer
        connectionFactory = new CachingConnectionFactory(
                rabbitMQ.getHost(),
                rabbitMQ.getAmqpPort()
        );

        connectionFactory.setUsername(rabbitMQ.getAdminUsername());
        connectionFactory.setPassword(rabbitMQ.getAdminPassword());

        // Enable publisher confirmations and returns
        connectionFactory.setPublisherConfirmType(
                CachingConnectionFactory.ConfirmType.CORRELATED
        );

        connectionFactory.setPublisherReturns(true);

        rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMandatory(true);

        // Declare RabbitMQ exchange
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);

        TopicExchange exchange = new TopicExchange(
                RabbitMQConfig.EVENT_EXCHANGE,
                true,
                false
        );

        Queue queue = new Queue(
                RabbitMQConfig.EVENT_QUEUE,
                true
        );

        Binding binding = BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(RabbitMQConfig.EVENT_ROUTING_KEY);

        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(binding);

        // Prepare repository and publisher
        outboxMessageRepository = mock(OutboxMessageRepository.class);

        outboxPublisher = new OutboxPublisher(
                outboxMessageRepository,
                rabbitTemplate
        );

        // Inject confirmation timeout because this test
        // creates the publisher without a Spring context
        ReflectionTestUtils.setField(
                outboxPublisher,
                "confirmTimeoutMs",
                5000L
        );
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    // TEST 1: Publish Outbox message to real RabbitMQ
    @Test
    void shouldPublishMessageToRabbitMQAndMarkItAsPublished() {

        String payload =
                "{\"bookingId\":125,\"ticketCount\":3}";

        OutboxMessage message = new OutboxMessage(
                "BOOKING",
                "125",
                "TICKET_ISSUED",
                payload
        );

        ReflectionTestUtils.setField(message, "id", 1L);

        when(outboxMessageRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(message));

        // Execute publisher
        outboxPublisher.publishPendingMessages();

        // Read actual message from RabbitMQ queue
        Message receivedMessage = rabbitTemplate.receive(
                RabbitMQConfig.EVENT_QUEUE,
                5000
        );

        assertNotNull(
                receivedMessage,
                "RabbitMQ queue should contain the published message"
        );

        String receivedPayload = new String(
                receivedMessage.getBody(),
                StandardCharsets.UTF_8
        );

        assertEquals(payload, receivedPayload);

        assertEquals(
                "1",
                receivedMessage.getMessageProperties().getMessageId()
        );

        assertEquals(
                "TICKET_ISSUED",
                receivedMessage.getMessageProperties()
                        .getHeaders()
                        .get("eventType")
        );

        assertEquals(
                "BOOKING",
                receivedMessage.getMessageProperties()
                        .getHeaders()
                        .get("aggregateType")
        );

        assertEquals(
                "125",
                receivedMessage.getMessageProperties()
                        .getHeaders()
                        .get("aggregateId")
        );

        // Verify the message was marked as published
        assertNotNull(message.getPublishedAt());

        verify(outboxMessageRepository).save(message);
    }
}
