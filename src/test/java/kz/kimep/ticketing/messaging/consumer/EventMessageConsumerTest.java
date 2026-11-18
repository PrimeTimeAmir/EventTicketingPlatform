package kz.kimep.ticketing.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kimep.ticketing.messaging.repo.ProcessedMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventMessageConsumerTest {

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    private EventMessageConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new EventMessageConsumer(
                processedMessageRepository,
                new ObjectMapper()
        );
    }

    @Test
    void shouldProcessNewTicketsIssuedMessage() {
        when(processedMessageRepository.insertIfAbsent("1"))
                .thenReturn(1);

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

        Message message = createMessage(payload, "TICKETS_ISSUED");

        assertDoesNotThrow(() -> consumer.consume(message));

        verify(processedMessageRepository).insertIfAbsent("1");
    }

    @Test
    void shouldIgnoreDuplicateMessage() {
        when(processedMessageRepository.insertIfAbsent("1"))
                .thenReturn(0);

        // The invalid JSON body also confirms that a duplicate
        // is skipped before its payload is parsed.
        Message message = createMessage(
                "this is not valid JSON",
                "TICKETS_ISSUED"
        );

        assertDoesNotThrow(() -> consumer.consume(message));

        verify(processedMessageRepository).insertIfAbsent("1");
    }

    @Test
    void shouldRejectInvalidTicketsIssuedPayload() {
        when(processedMessageRepository.insertIfAbsent("1"))
                .thenReturn(1);

        String payload = """
                {
                  "bookingId": 123,
                  "bookingReference": "BOOK-123",
                  "transactionId": "TX-123",
                  "eventId": 2,
                  "ticketCount": 2,
                  "ticketIds": [101]
                }
                """;

        Message message = createMessage(payload, "TICKETS_ISSUED");

        assertThrows(
                IllegalArgumentException.class,
                () -> consumer.consume(message)
        );

        verify(processedMessageRepository).insertIfAbsent("1");
    }

    private Message createMessage(String body, String eventType) {
        MessageProperties properties = new MessageProperties();

        properties.setMessageId("1");
        properties.setHeader("eventType", eventType);
        properties.setHeader("aggregateType", "BOOKING");
        properties.setHeader("aggregateId", "123");

        return new Message(
                body.getBytes(StandardCharsets.UTF_8),
                properties
        );
    }
}
