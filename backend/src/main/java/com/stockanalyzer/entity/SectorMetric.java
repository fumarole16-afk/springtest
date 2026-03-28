package com.stockanalyzer.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sector_metrics",
       uniqueConstraints = @UniqueConstraint(columnNames = {"sector_id", "date"}),
       indexes = @Index(columnList = "sector_id, date"))
public class SectorMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "avg_per", precision = 10, scale = 4)
    private BigDecimal avgPer;

    @Column(name = "avg_pbr", precision = 10, scale = 4)
    private BigDecimal avgPbr;

    @Column(name = "total_market_cap", precision = 20, scale = 2)
    private BigDecimal totalMarketCap;

    @Column(name = "avg_dividend_yield", precision = 10, scale = 4)
    private BigDecimal avgDividendYield;

    @Column(name = "top_gainers", columnDefinition = "TEXT")
    private String topGainers;

    @Column(name = "top_losers", columnDefinition = "TEXT")
    private String topLosers;

    public SectorMetric() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Sector getSector() { return sector; }
    public void setSector(Sector sector) { this.sector = sector; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getAvgPer() { return avgPer; }
    public void setAvgPer(BigDecimal avgPer) { this.avgPer = avgPer; }
    public BigDecimal getAvgPbr() { return avgPbr; }
    public void setAvgPbr(BigDecimal avgPbr) { this.avgPbr = avgPbr; }
    public BigDecimal getTotalMarketCap() { return totalMarketCap; }
    public void setTotalMarketCap(BigDecimal totalMarketCap) { this.totalMarketCap = totalMarketCap; }
    public BigDecimal getAvgDividendYield() { return avgDividendYield; }
    public void setAvgDividendYield(BigDecimal avgDividendYield) { this.avgDividendYield = avgDividendYield; }
    public String getTopGainers() { return topGainers; }
    public void setTopGainers(String topGainers) { this.topGainers = topGainers; }
    public String getTopLosers() { return topLosers; }
    public void setTopLosers(String topLosers) { this.topLosers = topLosers; }
}
