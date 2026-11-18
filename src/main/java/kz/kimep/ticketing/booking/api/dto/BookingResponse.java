package kz.kimep.ticketing.booking.api.dto;

import kz.kimep.ticketing.booking.domain.Booking;
import kz.kimep.ticketing.booking.domain.BookingStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

public record BookingResponse(
        Long id,
        String bookingReference,
        String holdReference,
        Long eventId,
        Set<Long> seatIds,
        BookingStatus status,
        BigDecimal subtotal,
        BigDecimal fees,
        BigDecimal total,
        String currency,
        OffsetDateTime createdAt
) {

    public static BookingResponse from(Booking booking) {

        return new BookingResponse(
                booking.getId(),
                booking.getBookingReference(),
                booking.getSeatHold().getHoldReference(),
                booking.getEventId(),
                Set.copyOf(booking.getSeatHold().getSeatIds()),
                booking.getStatus(),
                booking.getSubtotal(),
                booking.getFees(),
                booking.getTotal(),
                booking.getCurrency(),
                booking.getCreatedAt()
        );
    }
}