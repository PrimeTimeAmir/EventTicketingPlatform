package kz.kimep.ticketing.catalog.repo;

import kz.kimep.ticketing.catalog.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository
        extends JpaRepository<Venue, Long> {
}
