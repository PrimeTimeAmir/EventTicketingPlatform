package kz.kimep.ticketing.messaging.repo;

import kz.kimep.ticketing.messaging.domain.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxMessageRepository
        extends JpaRepository<OutboxMessage, Long> {

    List<OutboxMessage>
    findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
