package com.stockanalyzer.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stock_metrics",
       uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "date"}),
       indexes = {
           @Index(columnList = "stock_id, date"),
           @Index(columnList = "per"),
           @Index(columnList = "pbr"),
           @Index(columnList = "roe"),
           @Index(columnList = "debt_ratio")
       })
public class StockMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private LocalDate date;

    @Column(precision = 10, scale = 4)
    private BigDecimal per;

    @Column(precision = 10, scale = 4)
    private BigDecimal pbr;

    @Column(name = "dividend_yield", precision = 10, scale = 4)
    private BigDecimal dividendYield;

    @Column(name = "week52_high", precision = 12, scale = 4)
    private BigDecimal week52High;

    @Column(name = "week52_low", precision = 12, scale = 4)
    private BigDecimal week52Low;

    @Column(name = "avg_volume_30d")
    private Long avgVolume30d;

    @Column(name = "revenue_growth", precision = 10, scale = 4)
    private BigDecimal revenueGrowth;

    @Column(name = "operating_margin", precision = 10, scale = 4)
    private BigDecimal operatingMargin;

    @Column(precision = 10, scale = 4)
    private BigDecimal roe;

    @Column(name = "debt_ratio", precision = 10, scale = 4)
    private BigDecimal debtRatio;

    public StockMetric() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getPer() { return per; }
    public void setPer(BigDecimal per) { this.per = per; }
    public BigDecimal getPbr() { return pbr; }
    public void setPbr(BigDecimal pbr) { this.pbr = pbr; }
    public BigDecimal getDividendYield() { return dividendYield; }
    public void setDividendYield(BigDecimal dividendYield) { this.dividendYield = dividendYield; }
    public BigDecimal getWeek52High() { return week52High; }
    public void setWeek52High(BigDecimal week52High) { this.week52High = week52High; }
    public BigDecimal getWeek52Low() { return week52Low; }
    public void setWeek52Low(BigDecimal week52Low) { this.week52Low = week52Low; }
    public Long getAvgVolume30d() { return avgVolume30d; }
    public void setAvgVolume30d(Long avgVolume30d) { this.avgVolume30d = avgVolume30d; }
    public BigDecimal getRevenueGrowth() { return revenueGrowth; }
    public void setRevenueGrowth(BigDecimal revenueGrowth) { this.revenueGrowth = revenueGrowth; }
    public BigDecimal getOperatingMargin() { return operatingMargin; }
    public void setOperatingMargin(BigDecimal operatingMargin) { this.operatingMargin = operatingMargin; }
    public BigDecimal getRoe() { return roe; }
    public void setRoe(BigDecimal roe) { this.roe = roe; }
    public BigDecimal getDebtRatio() { return debtRatio; }
    public void setDebtRatio(BigDecimal debtRatio) { this.debtRatio = debtRatio; }
}
