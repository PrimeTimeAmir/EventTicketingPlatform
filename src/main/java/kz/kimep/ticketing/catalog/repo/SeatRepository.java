package kz.kimep.ticketing.catalog.repo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import kz.kimep.ticketing.catalog.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeatRepository
        extends JpaRepository<Seat, Long> {

    List<Seat> findAllBySection_Id(Long sectionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :seatIds ORDER BY s.id")
    List<Seat> findAllByIdInForUpdate(
            @Param("seatIds") Collection<Long> seatIds
    );
}