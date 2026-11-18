package kz.kimep.ticketing.payment.api.dto;

import kz.kimep.ticketing.booking.domain.Ticket;
import kz.kimep.ticketing.booking.domain.TicketStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TicketResponse(
        Long id,
        String ticketCode,
        Long bookingId,
        Long eventId,
        Long seatId,
        TicketStatus status,
        BigDecimal pricePaid,
        OffsetDateTime issuedAt
) {

    public static TicketResponse from(Ticket ticket) {

        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketCode(),
                ticket.getBooking().getId(),
                ticket.getEventId(),
                ticket.getSeatId(),
                ticket.getStatus(),
                ticket.getPricePaid(),
                ticket.getIssuedAt()
        );
    }
}