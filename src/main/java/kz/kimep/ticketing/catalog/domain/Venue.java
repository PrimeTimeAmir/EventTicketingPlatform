package kz.kimep.ticketing.catalog.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @OneToMany(
            mappedBy = "venue",
            fetch = FetchType.LAZY
    )
    private List<Section> sections = new ArrayList<>();

    // Constructor for Hibernate/JPA
    protected Venue() {
    }

    // Constructor for creating new venues
    public Venue(
            String name,
            String addressLine,
            String city,
            String timezone,
            Integer totalCapacity
    ) {
        this.name = name;
        this.addressLine = addressLine;
        this.city = city;
        this.timezone = timezone;
        this.totalCapacity = totalCapacity;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Integer getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Integer totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public List<Section> getSections() {
        return sections;
    }
}
