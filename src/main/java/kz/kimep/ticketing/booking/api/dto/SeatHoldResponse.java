package kz.kimep.ticketing.booking.api.dto;

import kz.kimep.ticketing.booking.domain.HoldStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

public record SeatHoldResponse(
        String holdReference,
        Long eventId,
        Set<Long> seatIds,
        OffsetDateTime expiresAt,
        HoldStatus status,
        BigDecimal subtotal,
        String currency
)
{
}