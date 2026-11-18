package kz.kimep.ticketing.messaging.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kimep.ticketing.messaging.domain.OutboxMessage;
import kz.kimep.ticketing.messaging.repo.OutboxMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(
            OutboxMessageRepository outboxMessageRepository,
            ObjectMapper objectMapper
    ) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.objectMapper = objectMapper;
    }

    // Save an event into the Outbox table
    @Transactional
    public OutboxMessage saveEvent(
            String aggregateType,
            String aggregateId,
            String eventType,
            Object payload
    ) {

        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException(
                    "Aggregate type cannot be empty"
            );
        }

        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException(
                    "Aggregate ID cannot be empty"
            );
        }

        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException(
                    "Event type cannot be empty"
            );
        }

        if (payload == null) {
            throw new IllegalArgumentException(
                    "Event payload cannot be null"
            );
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);

            OutboxMessage message = new OutboxMessage(
                    aggregateType,
                    aggregateId,
                    eventType,
                    jsonPayload
            );

            return outboxMessageRepository.save(message);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize Outbox event payload",
                    exception
            );
        }
    }
}
