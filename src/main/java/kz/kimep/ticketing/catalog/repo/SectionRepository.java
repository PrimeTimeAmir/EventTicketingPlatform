package kz.kimep.ticketing.catalog.repo;

import kz.kimep.ticketing.catalog.domain.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository
        extends JpaRepository<Section, Long> {

    List<Section> findAllByVenue_Id(Long venueId);
}