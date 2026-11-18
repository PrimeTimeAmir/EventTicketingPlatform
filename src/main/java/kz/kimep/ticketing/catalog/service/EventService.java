package kz.kimep.ticketing.catalog.service;

import kz.kimep.ticketing.support.audit.service.AuditService;
import kz.kimep.ticketing.catalog.api.dto.EventRequest;
import kz.kimep.ticketing.catalog.api.dto.EventResponse;
import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import kz.kimep.ticketing.catalog.domain.Venue;
import kz.kimep.ticketing.catalog.repo.EventRepository;
import kz.kimep.ticketing.catalog.repo.VenueRepository;
import kz.kimep.ticketing.identity.domain.Role;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public EventService(
            EventRepository eventRepository,
            VenueRepository venueRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // Create a new event
    @Transactional
    public EventResponse create(
            EventRequest request,
            String organizerEmail
    ) {

        validateSchedule(request);

        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Venue not found: " + request.venueId()
                ));

        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Organizer not found: " + organizerEmail
                ));

        Event event = new Event(
                venue,
                organizer,
                request.title().trim(),
                request.description() == null
                        ? null
                        : request.description().trim(),
                request.startsAt(),
                request.doorsOpenAt(),
                request.salesOpenAt(),
                request.salesCloseAt()
        );

        Event savedEvent = eventRepository.save(event);

        auditService.record(
                organizer.getId(),
                "EVENT_CREATED",
                "EVENT",
                savedEvent.getId().toString()
        );

        return EventResponse.from(savedEvent);
    }

    // Change event status
    @Transactional
    public EventResponse changeStatus(
            Long id,
            EventStatus newStatus,
            String organizerEmail
    ) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found: " + id
                ));

        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + organizerEmail
                ));

        // Organizers can manage only their own events.
        if (organizer.getRole() != Role.ADMIN
                && !event.getOrganizer().getId().equals(organizer.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot change another organizer's event"
            );
        }

        EventStatus currentStatus = event.getStatus();

        // Validate status transitions.
        boolean validTransition = switch (currentStatus) {
            case DRAFT ->
                    newStatus == EventStatus.PUBLISHED
                            || newStatus == EventStatus.CANCELLED;

            case PUBLISHED ->
                    newStatus == EventStatus.CANCELLED
                            || newStatus == EventStatus.COMPLETED;

            case CANCELLED, COMPLETED -> false;
        };

        if (!validTransition) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status transition: "
                            + currentStatus + " -> " + newStatus
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (newStatus == EventStatus.PUBLISHED) {

            if (!event.getStartsAt().isAfter(now)
                    || !event.getSalesCloseAt().isAfter(now)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot publish an event with expired schedule"
                );
            }
        }

        if (newStatus == EventStatus.COMPLETED
                && now.isBefore(event.getStartsAt())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot complete an event before it starts"
            );
        }

        event.setStatus(newStatus);

        String auditAction = switch (newStatus) {
            case PUBLISHED -> "EVENT_PUBLISHED";
            case CANCELLED -> "EVENT_CANCELLED";
            case COMPLETED -> "EVENT_COMPLETED";
            case DRAFT -> "EVENT_STATUS_CHANGED";
        };

        auditService.record(
                organizer.getId(),
                auditAction,
                "EVENT",
                event.getId().toString()
        );

        auditService.record(
                organizer.getId(),
                "EVENT_UPDATED",
                "EVENT",
                event.getId().toString()
        );

        return EventResponse.from(event);
    }

    // Update an existing event
    @Transactional
    public EventResponse update(
            Long id,
            EventRequest request,
            String organizerEmail
    ) {

        validateSchedule(request);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found: " + id
                ));

        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + organizerEmail
                ));

        // Organizers can update only their own events.
        if (organizer.getRole() != Role.ADMIN
                && !event.getOrganizer().getId().equals(organizer.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot update another organizer's event"
            );
        }

        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Venue not found: " + request.venueId()
                ));

        event.setVenue(venue);
        event.setTitle(request.title().trim());

        event.setDescription(
                request.description() == null
                        ? null
                        : request.description().trim()
        );

        event.setStartsAt(request.startsAt());
        event.setDoorsOpenAt(request.doorsOpenAt());
        event.setSalesOpenAt(request.salesOpenAt());
        event.setSalesCloseAt(request.salesCloseAt());

        return EventResponse.from(event);
    }

    // Get all events, optionally filtered by status
    @Transactional(readOnly = true)
    public Page<EventResponse> getAll(
            EventStatus status,
            Pageable pageable
    ) {

        if (status == null) {
            return eventRepository.findAll(pageable)
                    .map(EventResponse::from);
        }

        return eventRepository.findAllByStatus(status, pageable)
                .map(EventResponse::from);
    }

    // Get one event by ID
    @Transactional(readOnly = true)
    public EventResponse getById(Long id) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found: " + id
                ));

        return EventResponse.from(event);
    }

    // Validate event dates
    private void validateSchedule(EventRequest request) {

        OffsetDateTime now = OffsetDateTime.now();

        if (!request.startsAt().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Event start time must be in the future"
            );
        }

        if (request.doorsOpenAt().isAfter(request.startsAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Doors cannot open after the event starts"
            );
        }

        if (!request.salesOpenAt().isBefore(request.salesCloseAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sales opening time must be before sales closing time"
            );
        }

        if (request.salesCloseAt().isAfter(request.startsAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket sales must close before the event starts"
            );
        }
    }
}
