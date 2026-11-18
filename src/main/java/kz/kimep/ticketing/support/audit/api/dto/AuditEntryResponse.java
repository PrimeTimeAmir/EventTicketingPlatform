package kz.kimep.ticketing.support.audit.api.dto;

import kz.kimep.ticketing.support.audit.domain.AuditEntry;
import java.time.OffsetDateTime;

public record AuditEntryResponse(
        Long id,
        Long actorId,
        String action,
        String entityType,
        String entityId,
        OffsetDateTime createdAt
) {

    public static AuditEntryResponse from(AuditEntry entry) {
        return new AuditEntryResponse(
                entry.getId(),
                entry.getActorId(),
                entry.getAction(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getCreatedAt()
        );
    }
}