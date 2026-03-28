package com.stockanalyzer.entity;

import javax.persistence.*;

@Entity
@Table(name = "industries")
public class Industry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    public Industry() {}
    public Industry(String name, String description, Sector sector) {
        this.name = name;
        this.description = description;
        this.sector = sector;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Sector getSector() { return sector; }
    public void setSector(Sector sector) { this.sector = sector; }
}
