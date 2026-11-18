package kz.kimep.ticketing.booking.repo;

import kz.kimep.ticketing.booking.domain.Booking;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository
        extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String reference);

    Page<Booking> findAllByUserId(
            Long userId,
            Pageable pageable
    );

    Page<Booking> findAllByUserIdAndStatus(
            Long userId,
            BookingStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b
        FROM Booking b
        WHERE b.bookingReference = :reference
        """)
    Optional<Booking> findByBookingReferenceForUpdate(
            @Param("reference") String reference
    );

    boolean existsBySeatHold_Id(Long seatHoldId);
}
