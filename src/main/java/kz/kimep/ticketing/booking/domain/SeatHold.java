package kz.kimep.ticketing.booking.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "seat_holds")
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "hold_reference",
            nullable = false,
            unique = true,
            length = 64
    )
    private String holdReference;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "seat_hold_seats",
            joinColumns = @JoinColumn(name = "hold_id")
    )
    @Column(name = "seat_id", nullable = false)
    private Set<Long> seatIds = new HashSet<>();

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HoldStatus status = HoldStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SeatHold() {
    }

    public SeatHold(
            String holdReference,
            Long eventId,
            Long userId,
            Set<Long> seatIds,
            OffsetDateTime expiresAt
    ) {
        this.holdReference = holdReference;
        this.eventId = eventId;
        this.userId = userId;
        this.seatIds = new HashSet<>(seatIds);
        this.expiresAt = expiresAt;
        this.status = HoldStatus.ACTIVE;
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

    public String getHoldReference() {
        return holdReference;
    }

    public void setHoldReference(String holdReference) {
        this.holdReference = holdReference;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Set<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(Set<Long> seatIds) {
        this.seatIds = seatIds;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public void setStatus(HoldStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
