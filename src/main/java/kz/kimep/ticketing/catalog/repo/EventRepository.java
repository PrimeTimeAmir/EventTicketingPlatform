package kz.kimep.ticketing.catalog.repo;

import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventRepository
        extends JpaRepository<Event, Long> {

    Page<Event> findAllByStatus(
            EventStatus status,
            Pageable pageable
    );
}