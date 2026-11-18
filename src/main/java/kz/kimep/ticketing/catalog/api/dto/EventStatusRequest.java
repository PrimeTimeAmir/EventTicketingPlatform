package kz.kimep.ticketing.catalog.api.dto;

import jakarta.validation.constraints.NotNull;
import kz.kimep.ticketing.catalog.domain.EventStatus;

public record EventStatusRequest(

        @NotNull
        EventStatus status

)
{
}