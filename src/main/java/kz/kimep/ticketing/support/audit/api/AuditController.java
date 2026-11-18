package kz.kimep.ticketing.support.audit.api;

import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import kz.kimep.ticketing.support.audit.api.dto.AuditEntryResponse;
import kz.kimep.ticketing.support.audit.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;
    private final UserRepository userRepository;

    public AuditController(
            AuditService auditService,
            UserRepository userRepository
    ) {
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    // Get current user's audit history
    @GetMapping("/my")
    public Page<AuditEntryResponse> getMyHistory(
            Authentication authentication,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        return auditService
                .getActorHistory(user.getId(), pageable)
                .map(AuditEntryResponse::from);
    }

    // ADMIN only: Get all audit entries
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditEntryResponse> getAllHistory(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {

        return auditService
                .getAll(pageable)
                .map(AuditEntryResponse::from);
    }
}