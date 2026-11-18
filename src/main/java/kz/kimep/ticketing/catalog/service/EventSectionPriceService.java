package kz.kimep.ticketing.catalog.service;

import kz.kimep.ticketing.catalog.api.dto.EventSectionPriceRequest;
import kz.kimep.ticketing.catalog.api.dto.EventSectionPriceResponse;
import kz.kimep.ticketing.catalog.domain.Event;
import kz.kimep.ticketing.catalog.domain.EventSectionPrice;
import kz.kimep.ticketing.catalog.domain.EventStatus;
import kz.kimep.ticketing.catalog.domain.Section;
import kz.kimep.ticketing.catalog.repo.EventRepository;
import kz.kimep.ticketing.catalog.repo.EventSectionPriceRepository;
import kz.kimep.ticketing.catalog.repo.SectionRepository;
import kz.kimep.ticketing.identity.domain.Role;
import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class EventSectionPriceService {

    private final EventSectionPriceRepository priceRepository;
    private final EventRepository eventRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;

    public EventSectionPriceService(
            EventSectionPriceRepository priceRepository,
            EventRepository eventRepository,
            SectionRepository sectionRepository,
            UserRepository userRepository
    ) {
        this.priceRepository = priceRepository;
        this.eventRepository = eventRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
    }

    // Assign a ticket price to an event section
    @Transactional
    public EventSectionPriceResponse createPrice(
            Long eventId,
            EventSectionPriceRequest request,
            String organizerEmail
    ) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Event not found: " + eventId
                ));

        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + organizerEmail
                ));

        // Organizers can configure prices only for their own events.
        if (organizer.getRole() != Role.ADMIN
                && !event.getOrganizer().getId().equals(organizer.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot configure prices for another organizer's event"
            );
        }

        // Configure prices while the event is still a draft.
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Prices can only be configured for DRAFT events"
            );
        }

        Section section = sectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Section not found: " + request.sectionId()
                ));

        // The section must belong to the event's venue.
        if (!section.getVenue().getId().equals(event.getVenue().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Section does not belong to the event venue"
            );
        }

        // Do not create duplicate prices for the same event and section.
        if (priceRepository.existsByEvent_IdAndSection_Id(
                eventId,
                section.getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Price already exists for this event section"
            );
        }

        EventSectionPrice price = new EventSectionPrice(
                event,
                section,
                request.price()
        );

        EventSectionPrice savedPrice = priceRepository.save(price);

        return EventSectionPriceResponse.from(savedPrice);
    }

    // Get all ticket prices for an event
    @Transactional(readOnly = true)
    public List<EventSectionPriceResponse> getPricesByEvent(Long eventId) {

        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Event not found: " + eventId
            );
        }

        return priceRepository.findAllByEvent_Id(eventId)
                .stream()
                .map(EventSectionPriceResponse::from)
                .toList();
    }
}
