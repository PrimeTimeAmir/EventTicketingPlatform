package kz.kimep.ticketing.catalog.api.dto;

import kz.kimep.ticketing.catalog.domain.EventSectionPrice;
import java.math.BigDecimal;

public record EventSectionPriceResponse(

        Long id,
        Long eventId,
        Long sectionId,
        String sectionName,
        BigDecimal price,
        String currency

) {

    public static EventSectionPriceResponse from(
            EventSectionPrice eventSectionPrice
    ) {
        return new EventSectionPriceResponse(
                eventSectionPrice.getId(),
                eventSectionPrice.getEvent().getId(),
                eventSectionPrice.getSection().getId(),
                eventSectionPrice.getSection().getName(),
                eventSectionPrice.getPrice(),
                eventSectionPrice.getCurrency()
        );
    }
}