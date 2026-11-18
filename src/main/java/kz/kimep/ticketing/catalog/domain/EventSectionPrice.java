package kz.kimep.ticketing.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "event_section_prices",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_event_section_price",
                        columnNames = {"event_id", "section_id"}
                )
        }
)
public class EventSectionPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency = "KZT";

    protected EventSectionPrice() {
    }

    public EventSectionPrice(
            Event event,
            Section section,
            BigDecimal price
    ) {
        this.event = event;
        this.section = section;
        this.price = price;
        this.currency = "KZT";
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
