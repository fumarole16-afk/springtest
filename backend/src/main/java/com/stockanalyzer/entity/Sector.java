package com.stockanalyzer.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sectors")
public class Sector {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @OneToMany(mappedBy = "sector")
    private List<Industry> industries = new ArrayList<>();

    public Sector() {}
    public Sector(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Industry> getIndustries() { return industries; }
    public void setIndustries(List<Industry> industries) { this.industries = industries; }
}
