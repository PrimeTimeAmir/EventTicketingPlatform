package kz.kimep.ticketing.support.audit.service;

import kz.kimep.ticketing.support.audit.domain.AuditEntry;
import kz.kimep.ticketing.support.audit.repo.AuditEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEntryRepository auditEntryRepository;

    public AuditService(AuditEntryRepository auditEntryRepository) {
        this.auditEntryRepository = auditEntryRepository;
    }

    @Transactional
    public AuditEntry record(
            Long actorId,
            String action,
            String entityType,
            String entityId
    ) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException(
                    "Audit action cannot be empty"
            );
        }

        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException(
                    "Audit entity type cannot be empty"
            );
        }

        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException(
                    "Audit entity ID cannot be empty"
            );
        }

        AuditEntry entry = new AuditEntry(
                actorId,
                action,
                entityType,
                entityId
        );

        return auditEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditEntry> getActorHistory(
            Long actorId,
            Pageable pageable
    ) {
        if (actorId == null) {
            throw new IllegalArgumentException(
                    "Actor ID cannot be null"
            );
        }

        return auditEntryRepository.findAllByActorId(
                actorId,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<AuditEntry> getAll(Pageable pageable) {
        return auditEntryRepository.findAll(pageable);
    }
}