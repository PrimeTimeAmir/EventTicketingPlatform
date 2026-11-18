package kz.kimep.ticketing.booking.repo;

import kz.kimep.ticketing.booking.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository
        extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String code);

    List<Ticket> findAllByBooking_Id(Long bookingId);

    boolean existsByEventIdAndSeatId(
            Long eventId,
            Long seatId
    );

    List<Ticket> findAllByEventIdAndSeatIdIn(
            Long eventId,
            Collection<Long> seatIds
    );

    List<Ticket> findAllByBooking_UserIdOrderByIssuedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT t
        FROM Ticket t
        WHERE t.ticketCode = :ticketCode
        """)
    Optional<Ticket> findByTicketCodeForUpdate(
            @Param("ticketCode") String ticketCode
    );
}
