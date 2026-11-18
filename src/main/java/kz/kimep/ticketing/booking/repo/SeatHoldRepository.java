package kz.kimep.ticketing.booking.repo;

import java.time.OffsetDateTime;
import kz.kimep.ticketing.booking.domain.SeatHold;
import kz.kimep.ticketing.booking.domain.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatHoldRepository
        extends JpaRepository<SeatHold, Long> {

    Optional<SeatHold> findByHoldReference(String reference);

    List<SeatHold> findAllByUserIdAndStatus(
            Long userId,
            HoldStatus status
    );

    List<SeatHold> findAllByEventIdAndStatusAndExpiresAtAfter(
            Long eventId,
            HoldStatus status,
            OffsetDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SeatHold h
        SET h.status = :expiredStatus
        WHERE h.status = :activeStatus
          AND h.expiresAt <= :now
        """)
    int expireHolds(
            @Param("now") OffsetDateTime now,
            @Param("activeStatus") HoldStatus activeStatus,
            @Param("expiredStatus") HoldStatus expiredStatus
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT h
        FROM SeatHold h
        WHERE h.holdReference = :reference
        """)
    Optional<SeatHold> findByHoldReferenceForUpdate(
            @Param("reference") String reference
    );
}
