package kz.kimep.ticketing.booking.api.dto;

import kz.kimep.ticketing.booking.domain.Ticket;
import kz.kimep.ticketing.booking.domain.TicketStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TicketDetailsResponse(
        Long id,
        String ticketCode,
        Long bookingId,
        Long eventId,
        Long seatId,
        TicketStatus status,
        BigDecimal pricePaid,
        OffsetDateTime issuedAt,
        OffsetDateTime checkedInAt
) {

    public static TicketDetailsResponse from(Ticket ticket) {
        return new TicketDetailsResponse(
                ticket.getId(),
                ticket.getTicketCode(),
                ticket.getBooking().getId(),
                ticket.getEventId(),
                ticket.getSeatId(),
                ticket.getStatus(),
                ticket.getPricePaid(),
                ticket.getIssuedAt(),
                ticket.getCheckedInAt()
        );
    }
}