package kz.kimep.ticketing.catalog.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sections")
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "row_count", nullable = false)
    private Integer rowCount;

    @Column(name = "seats_per_row", nullable = false)
    private Integer seatsPerRow;

    @Column(name = "price_tier", nullable = false, length = 50)
    private String priceTier;

    @OneToMany(
            mappedBy = "section",
            fetch = FetchType.LAZY
    )
    private List<Seat> seats = new ArrayList<>();

    protected Section() {
    }

    public Section(
            Venue venue,
            String name,
            Integer rowCount,
            Integer seatsPerRow,
            String priceTier
    ) {
        this.venue = venue;
        this.name = name;
        this.rowCount = rowCount;
        this.seatsPerRow = seatsPerRow;
        this.priceTier = priceTier;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Integer getSeatsPerRow() {
        return seatsPerRow;
    }

    public void setSeatsPerRow(Integer seatsPerRow) {
        this.seatsPerRow = seatsPerRow;
    }

    public String getPriceTier() {
        return priceTier;
    }

    public void setPriceTier(String priceTier) {
        this.priceTier = priceTier;
    }

    public List<Seat> getSeats() {
        return seats;
    }
}
