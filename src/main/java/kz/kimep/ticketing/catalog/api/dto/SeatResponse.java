package kz.kimep.ticketing.catalog.api.dto;

import kz.kimep.ticketing.catalog.domain.Seat;

public record SeatResponse(

        Long id,
        Long sectionId,
        String rowLabel,
        Integer seatNumber,
        boolean accessible

) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getSection().getId(),
                seat.getRowLabel(),
                seat.getSeatNumber(),
                seat.isAccessible()
        );
    }
}