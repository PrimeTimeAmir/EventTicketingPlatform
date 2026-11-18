package kz.kimep.ticketing.payment.api.dto;

import kz.kimep.ticketing.payment.domain.Payment;
import kz.kimep.ticketing.payment.domain.PaymentMethod;
import kz.kimep.ticketing.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(
        Long id,
        String transactionId,
        String bookingReference,
        Long bookingId,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String failureCode,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt
) {

    public static PaymentResponse from(Payment payment) {

        return new PaymentResponse(
                payment.getId(),
                payment.getTransactionId(),
                payment.getBooking().getBookingReference(),
                payment.getBooking().getId(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getFailureCode(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}