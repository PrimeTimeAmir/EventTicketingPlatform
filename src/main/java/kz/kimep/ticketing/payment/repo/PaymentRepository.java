package kz.kimep.ticketing.payment.repo;

import kz.kimep.ticketing.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByBooking_Id(Long bookingId);

    boolean existsByBooking_Id(Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.transactionId = :transactionId
            """)
    Optional<Payment> findByTransactionIdForUpdate(
            @Param("transactionId") String transactionId
    );
}