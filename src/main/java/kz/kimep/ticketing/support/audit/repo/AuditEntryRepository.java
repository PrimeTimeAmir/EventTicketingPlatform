package kz.kimep.ticketing.support.audit.repo;

import kz.kimep.ticketing.support.audit.domain.AuditEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEntryRepository
        extends JpaRepository<AuditEntry, Long> {

    Page<AuditEntry> findAllByActorId(
            Long actorId,
            Pageable pageable
    );
}
