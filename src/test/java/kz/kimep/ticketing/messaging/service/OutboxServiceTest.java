package kz.kimep.ticketing.messaging.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.kimep.ticketing.messaging.domain.OutboxMessage;
import kz.kimep.ticketing.messaging.repo.OutboxMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    private ObjectMapper objectMapper;
    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        outboxService = new OutboxService(
                outboxMessageRepository,
                objectMapper
        );
    }

    // TEST 1: Save event successfully
    @Test
    void shouldSaveEventSuccessfully() throws Exception {

        when(outboxMessageRepository.save(any(OutboxMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> payload = Map.of(
                "bookingId", 125,
                "ticketCount", 3
        );

        OutboxMessage result = outboxService.saveEvent(
                "BOOKING",
                "125",
                "TICKET_ISSUED",
                payload
        );

        ArgumentCaptor<OutboxMessage> captor =
                ArgumentCaptor.forClass(OutboxMessage.class);

        verify(outboxMessageRepository).save(captor.capture());

        OutboxMessage savedMessage = captor.getValue();

        assertNotNull(result);
        assertEquals("BOOKING", savedMessage.getAggregateType());
        assertEquals("125", savedMessage.getAggregateId());
        assertEquals("TICKET_ISSUED", savedMessage.getEventType());

        JsonNode json = objectMapper.readTree(savedMessage.getPayload());

        assertEquals(125, json.get("bookingId").asInt());
        assertEquals(3, json.get("ticketCount").asInt());
    }

    // TEST 2: Reject empty aggregate type
    @Test
    void shouldRejectEmptyAggregateType() {

        assertThrows(
                IllegalArgumentException.class,
                () -> outboxService.saveEvent(
                        "",
                        "125",
                        "TICKET_ISSUED",
                        Map.of("bookingId", 125)
                )
        );

        verifyNoInteractions(outboxMessageRepository);
    }

    // TEST 3: Reject empty aggregate ID
    @Test
    void shouldRejectEmptyAggregateId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> outboxService.saveEvent(
                        "BOOKING",
                        " ",
                        "TICKET_ISSUED",
                        Map.of("bookingId", 125)
                )
        );

        verifyNoInteractions(outboxMessageRepository);
    }

    // TEST 4: Reject empty event type
    @Test
    void shouldRejectEmptyEventType() {

        assertThrows(
                IllegalArgumentException.class,
                () -> outboxService.saveEvent(
                        "BOOKING",
                        "125",
                        "",
                        Map.of("bookingId", 125)
                )
        );

        verifyNoInteractions(outboxMessageRepository);
    }

    // TEST 5: Reject null payload
    @Test
    void shouldRejectNullPayload() {

        assertThrows(
                IllegalArgumentException.class,
                () -> outboxService.saveEvent(
                        "BOOKING",
                        "125",
                        "TICKET_ISSUED",
                        null
                )
        );

        verifyNoInteractions(outboxMessageRepository);
    }
}
