package kz.kimep.ticketing.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class DatabaseMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayShouldCreateAllExpectedTables() {

        List<String> expectedTables = List.of(
                "users",
                "refresh_tokens",
                "venues",
                "sections",
                "seats",
                "events",
                "event_section_prices",
                "seat_holds",
                "seat_hold_seats",
                "bookings",
                "tickets",
                "payments",
                "outbox_messages",
                "processed_messages",
                "audit_entries"
        );

        for (String table : expectedTables) {

            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_name = ?
                    """,
                    Integer.class,
                    table
            );

            assertNotNull(count);
            assertEquals(
                    1,
                    count,
                    "Database table should exist: " + table
            );
        }
    }

    @Test
    void allSixFlywayMigrationsShouldBeApplied() {

        String version = jdbcTemplate.queryForObject(
                """
                SELECT version
                FROM flyway_schema_history
                WHERE success = TRUE
                ORDER BY installed_rank DESC
                LIMIT 1
                """,
                String.class
        );

        assertEquals("6", version);
    }
}
