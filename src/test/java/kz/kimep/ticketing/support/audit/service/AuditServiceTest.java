package kz.kimep.ticketing.support.audit.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import kz.kimep.ticketing.support.audit.domain.AuditEntry;
import kz.kimep.ticketing.support.audit.repo.AuditEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEntryRepository auditEntryRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void recordSavesAuditEntry() {
        when(auditEntryRepository.save(any(AuditEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditEntry result = auditService.record(
                1L,
                "EVENT_CREATED",
                "EVENT",
                "25"
        );

        assertNotNull(result);
        assertEquals(1L, result.getActorId());
        assertEquals("EVENT_CREATED", result.getAction());
        assertEquals("EVENT", result.getEntityType());
        assertEquals("25", result.getEntityId());

        verify(auditEntryRepository, times(1))
                .save(any(AuditEntry.class));
    }

    @Test
    void recordAllowsNullActorId() {
        when(auditEntryRepository.save(any(AuditEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditEntry result = auditService.record(
                null,
                "SYSTEM_ACTION",
                "BOOKING",
                "10"
        );

        assertNull(result.getActorId());
        assertEquals("SYSTEM_ACTION", result.getAction());

        verify(auditEntryRepository).save(any(AuditEntry.class));
    }

    @Test
    void recordRejectsEmptyAction() {
        assertThrows(
                IllegalArgumentException.class,
                () -> auditService.record(
                        1L,
                        "",
                        "EVENT",
                        "25"
                )
        );

        verify(auditEntryRepository, never())
                .save(any(AuditEntry.class));
    }

    @Test
    void recordRejectsEmptyEntityType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> auditService.record(
                        1L,
                        "EVENT_CREATED",
                        "",
                        "25"
                )
        );

        verify(auditEntryRepository, never())
                .save(any(AuditEntry.class));
    }

    @Test
    void recordRejectsEmptyEntityId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> auditService.record(
                        1L,
                        "EVENT_CREATED",
                        "EVENT",
                        ""
                )
        );

        verify(auditEntryRepository, never())
                .save(any(AuditEntry.class));
    }

    @Test
    void getAllReturnsPagedAuditEntries() {
        Pageable pageable = PageRequest.of(0, 20);

        AuditEntry entry = new AuditEntry(
                1L,
                "EVENT_CREATED",
                "EVENT",
                "25"
        );

        Page<AuditEntry> expectedPage =
                new PageImpl<>(List.of(entry), pageable, 1);

        when(auditEntryRepository.findAll(pageable))
                .thenReturn(expectedPage);

        Page<AuditEntry> result = auditService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("EVENT_CREATED", result.getContent().get(0).getAction());

        verify(auditEntryRepository).findAll(pageable);
    }
}