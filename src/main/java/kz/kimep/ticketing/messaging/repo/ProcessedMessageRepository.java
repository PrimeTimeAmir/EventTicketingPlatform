package kz.kimep.ticketing.messaging.repo;

import kz.kimep.ticketing.messaging.domain.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedMessageRepository
        extends JpaRepository<ProcessedMessage, Long> {

    boolean existsByMessageId(String messageId);

    long countByMessageId(String messageId);

    @Modifying
    @Query(value = """
            INSERT INTO processed_messages (message_id, consumed_at)
            VALUES (:messageId, CURRENT_TIMESTAMP)
            ON CONFLICT (message_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("messageId") String messageId);
}
