package kz.kimep.ticketing.messaging.service;

import kz.kimep.ticketing.messaging.domain.OutboxMessage;
import kz.kimep.ticketing.messaging.repo.OutboxMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "ticketing.outbox.publish-interval-ms=3600000",
        "ticketing.messaging.consumer-enabled=false",
        "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})

@Testcontainers
class OutboxTransactionIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanDatabase() {
        outboxMessageRepository.deleteAll();
    }

    // TEST 1: Outbox event is saved in PostgreSQL
    @Test
    void shouldPersistOutboxEventInDatabase() {

        OutboxMessage saved = outboxService.saveEvent(
                "BOOKING",
                "456",
                "TICKETS_ISSUED",
                java.util.Map.of(
                        "bookingId", 456,
                        "ticketCount", 2
                )
        );

        assertNotNull(saved.getId());

        OutboxMessage loaded = outboxMessageRepository
                .findById(saved.getId())
                .orElseThrow();

        assertEquals("BOOKING", loaded.getAggregateType());
        assertEquals("456", loaded.getAggregateId());
        assertEquals("TICKETS_ISSUED", loaded.getEventType());

        assertNotNull(loaded.getPayload());
        assertNull(loaded.getPublishedAt());
    }

    // TEST 2: Outbox event rolls back with outer transaction
    @Test
    void shouldRollbackOutboxEventWithOuterTransaction() {

        long initialCount = outboxMessageRepository.count();

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        assertThrows(
                IllegalStateException.class,
                () -> transactionTemplate.execute(status -> {

                    outboxService.saveEvent(
                            "BOOKING",
                            "789",
                            "TICKETS_ISSUED",
                            java.util.Map.of(
                                    "bookingId", 789,
                                    "ticketCount", 3
                            )
                    );

                    throw new IllegalStateException(
                            "Force transaction rollback"
                    );
                })
        );

        assertEquals(
                initialCount,
                outboxMessageRepository.count()
        );
    }
}
