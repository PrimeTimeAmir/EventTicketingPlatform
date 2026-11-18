package kz.kimep.ticketing.messaging.consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.test.annotation.DirtiesContext;
import kz.kimep.ticketing.messaging.config.RabbitMQConfig;
import kz.kimep.ticketing.messaging.repo.ProcessedMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(properties = {
        "ticketing.outbox.publish-interval-ms=3600000",
        "ticketing.messaging.consumer-enabled=true",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EventMessageConsumerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer("rabbitmq:3-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private EventMessageConsumer eventMessageConsumer;

    @Autowired
    private RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @AfterAll
    void stopRabbitListeners() {
        rabbitListenerEndpointRegistry.stop();
    }

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    @Test
    void shouldConsumeRabbitMessageAndSaveItToDatabase()
            throws InterruptedException {

        String messageId = "integration-consumer-001";

        String payload = """
                {
                  "bookingId": 123,
                  "bookingReference": "BOOK-123",
                  "transactionId": "TX-123",
                  "eventId": 2,
                  "ticketCount": 2,
                  "ticketIds": [101, 102]
                }
                """;

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.EVENT_ROUTING_KEY,
                payload,
                message -> {
                    message.getMessageProperties()
                            .setMessageId(messageId);

                    message.getMessageProperties()
                            .setHeader("eventType", "TICKETS_ISSUED");

                    message.getMessageProperties()
                            .setHeader("aggregateType", "BOOKING");

                    message.getMessageProperties()
                            .setHeader("aggregateId", "123");

                    return message;
                }
        );

        boolean processed = waitForMessage(messageId);

        assertTrue(
                processed,
                "Consumer did not save the message to processed_messages"
        );

        assertTrue(
                processedMessageRepository.existsByMessageId(messageId),
                "Processed message should exist in PostgreSQL"
        );
    }


    @Test
    void shouldRollbackProcessedMessageWhenPayloadIsInvalid() {

        String messageId = "rollback-check-001";

        Message message = createMessage(
                messageId,
                "this is not valid JSON"
        );

        assertThrows(
                JsonProcessingException.class,
                () -> eventMessageConsumer.consume(message)
        );

        assertFalse(
                processedMessageRepository.existsByMessageId(messageId),
                "Invalid message must not be marked as processed"
        );
    }

    @Test
    void shouldNotProcessSameMessageTwice() throws Exception {

        String messageId = "duplicate-check-001";

        String payload = """
            {
              "bookingId": 456,
              "bookingReference": "BOOK-456",
              "transactionId": "TX-456",
              "eventId": 2,
              "ticketCount": 2,
              "ticketIds": [201, 202]
            }
            """;

        Message message = createMessage(messageId, payload);

        // First delivery
        eventMessageConsumer.consume(message);

        // Second delivery with exactly the same ID
        eventMessageConsumer.consume(message);

        assertEquals(
                1,
                processedMessageRepository.countByMessageId(messageId),
                "The message must be registered only once"
        );
    }


    @Test
    void shouldSendInvalidMessageToDeadLetterQueue()
            throws InterruptedException {

        String messageId = "dead-letter-check-001";

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EVENT_EXCHANGE,
                RabbitMQConfig.EVENT_ROUTING_KEY,
                "this is not valid JSON",
                message -> {
                    message.getMessageProperties()
                            .setMessageId(messageId);

                    message.getMessageProperties()
                            .setHeader("eventType", "TICKETS_ISSUED");

                    message.getMessageProperties()
                            .setHeader("aggregateType", "BOOKING");

                    message.getMessageProperties()
                            .setHeader("aggregateId", "123");

                    return message;
                }
        );

        Message deadLetterMessage = waitForDeadLetterMessage();

        assertNotNull(
                deadLetterMessage,
                "Invalid message should be sent to the dead letter queue"
        );

        assertEquals(
                messageId,
                deadLetterMessage.getMessageProperties().getMessageId()
        );

        assertFalse(
                processedMessageRepository.existsByMessageId(messageId),
                "Invalid message must not be marked as processed"
        );
    }

    private Message waitForDeadLetterMessage()
            throws InterruptedException {

        long deadline = System.currentTimeMillis() + 10_000;

        while (System.currentTimeMillis() < deadline) {

            Message message =
                    rabbitTemplate.receive(RabbitMQConfig.DEAD_LETTER_QUEUE);

            if (message != null) {
                return message;
            }

            Thread.sleep(100);
        }

        return null;
    }

    private Message createMessage(String messageId, String payload) {

        MessageProperties properties = new MessageProperties();

        properties.setMessageId(messageId);
        properties.setHeader("eventType", "TICKETS_ISSUED");
        properties.setHeader("aggregateType", "BOOKING");
        properties.setHeader("aggregateId", "123");

        return new Message(
                payload.getBytes(StandardCharsets.UTF_8),
                properties
        );
    }


    private boolean waitForMessage(String messageId)
            throws InterruptedException {

        long deadline = System.currentTimeMillis() + 10_000;

        while (System.currentTimeMillis() < deadline) {
            if (processedMessageRepository.existsByMessageId(messageId)) {
                return true;
            }

            Thread.sleep(100);
        }

        return false;
    }
}
