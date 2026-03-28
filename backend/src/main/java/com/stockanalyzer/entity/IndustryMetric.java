package com.stockanalyzer.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "industry_metrics",
       uniqueConstraints = @UniqueConstraint(columnNames = {"industry_id", "date"}),
       indexes = @Index(columnList = "industry_id, date"))
public class IndustryMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_id", nullable = false)
    private Industry industry;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "avg_per", precision = 10, scale = 4)
    private BigDecimal avgPer;

    @Column(name = "avg_pbr", precision = 10, scale = 4)
    private BigDecimal avgPbr;

    @Column(name = "total_market_cap", precision = 20, scale = 2)
    private BigDecimal totalMarketCap;

    @Column(name = "stock_count")
    private Integer stockCount;

    @Column(name = "performance_1d", precision = 10, scale = 4)
    private BigDecimal performance1d;

    @Column(name = "performance_1w", precision = 10, scale = 4)
    private BigDecimal performance1w;

    @Column(name = "performance_1m", precision = 10, scale = 4)
    private BigDecimal performance1m;

    @Column(name = "performance_3m", precision = 10, scale = 4)
    private BigDecimal performance3m;

    @Column(name = "performance_1y", precision = 10, scale = 4)
    private BigDecimal performance1y;

    public IndustryMetric() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Industry getIndustry() { return industry; }
    public void setIndustry(Industry industry) { this.industry = industry; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getAvgPer() { return avgPer; }
    public void setAvgPer(BigDecimal avgPer) { this.avgPer = avgPer; }
    public BigDecimal getAvgPbr() { return avgPbr; }
    public void setAvgPbr(BigDecimal avgPbr) { this.avgPbr = avgPbr; }
    public BigDecimal getTotalMarketCap() { return totalMarketCap; }
    public void setTotalMarketCap(BigDecimal totalMarketCap) { this.totalMarketCap = totalMarketCap; }
    public Integer getStockCount() { return stockCount; }
    public void setStockCount(Integer stockCount) { this.stockCount = stockCount; }
    public BigDecimal getPerformance1d() { return performance1d; }
    public void setPerformance1d(BigDecimal performance1d) { this.performance1d = performance1d; }
    public BigDecimal getPerformance1w() { return performance1w; }
    public void setPerformance1w(BigDecimal performance1w) { this.performance1w = performance1w; }
    public BigDecimal getPerformance1m() { return performance1m; }
    public void setPerformance1m(BigDecimal performance1m) { this.performance1m = performance1m; }
    public BigDecimal getPerformance3m() { return performance3m; }
    public void setPerformance3m(BigDecimal performance3m) { this.performance3m = performance3m; }
    public BigDecimal getPerformance1y() { return performance1y; }
    public void setPerformance1y(BigDecimal performance1y) { this.performance1y = performance1y; }
}
