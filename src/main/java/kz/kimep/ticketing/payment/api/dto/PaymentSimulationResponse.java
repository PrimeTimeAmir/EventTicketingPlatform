package kz.kimep.ticketing.payment.api.dto;

import kz.kimep.ticketing.booking.domain.BookingStatus;
import java.util.List;

public record PaymentSimulationResponse(
        PaymentResponse payment,
        BookingStatus bookingStatus,
        List<TicketResponse> tickets
)
{
}