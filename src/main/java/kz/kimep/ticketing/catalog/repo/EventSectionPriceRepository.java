package kz.kimep.ticketing.catalog.repo;

import kz.kimep.ticketing.catalog.domain.EventSectionPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventSectionPriceRepository
        extends JpaRepository<EventSectionPrice, Long> {

    List<EventSectionPrice> findAllByEvent_Id(Long eventId);

    boolean existsByEvent_IdAndSection_Id(
            Long eventId,
            Long sectionId
    );
}