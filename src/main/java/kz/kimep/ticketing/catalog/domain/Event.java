package kz.kimep.ticketing.catalog.domain;

import jakarta.persistence.*;
import kz.kimep.ticketing.identity.domain.User;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "doors_open_at", nullable = false)
    private OffsetDateTime doorsOpenAt;

    @Column(name = "sales_open_at", nullable = false)
    private OffsetDateTime salesOpenAt;

    @Column(name = "sales_close_at", nullable = false)
    private OffsetDateTime salesCloseAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(
            mappedBy = "event",
            fetch = FetchType.LAZY
    )
    private List<EventSectionPrice> sectionPrices = new ArrayList<>();

    protected Event() {
    }

    public Event(
            Venue venue,
            User organizer,
            String title,
            String description,
            OffsetDateTime startsAt,
            OffsetDateTime doorsOpenAt,
            OffsetDateTime salesOpenAt,
            OffsetDateTime salesCloseAt
    ) {
        this.venue = venue;
        this.organizer = organizer;
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.doorsOpenAt = doorsOpenAt;
        this.salesOpenAt = salesOpenAt;
        this.salesCloseAt = salesCloseAt;
        this.status = EventStatus.DRAFT;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Venue getVenue() {
        return venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public User getOrganizer() {
        return organizer;
    }

    public void setOrganizer(User organizer) {
        this.organizer = organizer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(OffsetDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public OffsetDateTime getDoorsOpenAt() {
        return doorsOpenAt;
    }

    public void setDoorsOpenAt(OffsetDateTime doorsOpenAt) {
        this.doorsOpenAt = doorsOpenAt;
    }

    public OffsetDateTime getSalesOpenAt() {
        return salesOpenAt;
    }

    public void setSalesOpenAt(OffsetDateTime salesOpenAt) {
        this.salesOpenAt = salesOpenAt;
    }

    public OffsetDateTime getSalesCloseAt() {
        return salesCloseAt;
    }

    public void setSalesCloseAt(OffsetDateTime salesCloseAt) {
        this.salesCloseAt = salesCloseAt;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public List<EventSectionPrice> getSectionPrices() {
        return sectionPrices;
    }
}
