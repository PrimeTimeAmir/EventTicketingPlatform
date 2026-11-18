package kz.kimep.ticketing.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kimep.ticketing.messaging.config.RabbitMQConfig;
import kz.kimep.ticketing.messaging.repo.ProcessedMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;

@Component
public class EventMessageConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(EventMessageConsumer.class);

    private final ProcessedMessageRepository processedMessageRepository;
    private final ObjectMapper objectMapper;

    public EventMessageConsumer(
            ProcessedMessageRepository processedMessageRepository,
            ObjectMapper objectMapper
    ) {
        this.processedMessageRepository = processedMessageRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(
            queues = RabbitMQConfig.EVENT_QUEUE,
            autoStartup = "${ticketing.messaging.consumer-enabled:true}"
    )
    public void consume(Message message) throws JsonProcessingException {

        MessageProperties properties = message.getMessageProperties();

        String messageId = properties.getMessageId();
        String eventType = getRequiredHeader(properties, "eventType");
        String aggregateType = getRequiredHeader(properties, "aggregateType");
        String aggregateId = getRequiredHeader(properties, "aggregateId");

        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException(
                    "RabbitMQ message ID is missing"
            );
        }

        String payloadJson =
                new String(message.getBody(), StandardCharsets.UTF_8);

        // Atomically register the message.
        // If it was already processed, skip it.
        int inserted =
                processedMessageRepository.insertIfAbsent(messageId);

        if (inserted == 0) {
            log.info(
                    "Duplicate message ignored: messageId={}",
                    messageId
            );
            return;
        }

        JsonNode payload = objectMapper.readTree(payloadJson);

        switch (eventType) {
            case "TICKETS_ISSUED" ->
                    handleTicketsIssued(
                            messageId,
                            aggregateType,
                            aggregateId,
                            payload
                    );

            default -> throw new IllegalArgumentException(
                    "Unsupported event type: " + eventType
            );
        }
    }

    private void handleTicketsIssued(
            String messageId,
            String aggregateType,
            String aggregateId,
            JsonNode payload
    ) {
        if (!payload.hasNonNull("bookingId")
                || !payload.hasNonNull("bookingReference")
                || !payload.hasNonNull("transactionId")
                || !payload.hasNonNull("eventId")
                || !payload.hasNonNull("ticketCount")
                || !payload.path("ticketIds").isArray()) {

            throw new IllegalArgumentException(
                    "Invalid TICKETS_ISSUED event payload"
            );
        }

        int ticketCount = payload.get("ticketCount").asInt();
        int actualTicketCount = payload.get("ticketIds").size();

        if (ticketCount < 1 || ticketCount != actualTicketCount) {
            throw new IllegalArgumentException(
                    "Ticket count does not match ticket IDs"
            );
        }

        log.info(
                "Tickets issued event consumed: messageId={}, " +
                        "aggregateType={}, aggregateId={}, " +
                        "bookingId={}, ticketCount={}",
                messageId,
                aggregateType,
                aggregateId,
                payload.get("bookingId").asLong(),
                ticketCount
        );
    }

    private String getRequiredHeader(
            MessageProperties properties,
            String headerName
    ) {
        Object value = properties.getHeader(headerName);

        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(
                    "Missing RabbitMQ header: " + headerName
            );
        }

        return value.toString();
    }
}
