package com.stockanalyzer.entity;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "financials",
       uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "period"}))
public class Financial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false, length = 10)
    private String period;

    @Column(nullable = false, length = 10)
    private String type;

    @Column(precision = 20, scale = 2)
    private BigDecimal revenue;

    @Column(name = "operating_income", precision = 20, scale = 2)
    private BigDecimal operatingIncome;

    @Column(name = "net_income", precision = 20, scale = 2)
    private BigDecimal netIncome;

    @Column(name = "total_assets", precision = 20, scale = 2)
    private BigDecimal totalAssets;

    @Column(name = "total_liabilities", precision = 20, scale = 2)
    private BigDecimal totalLiabilities;

    @Column(name = "total_equity", precision = 20, scale = 2)
    private BigDecimal totalEquity;

    @Column(name = "operating_cash_flow", precision = 20, scale = 2)
    private BigDecimal operatingCashFlow;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    public Financial() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    public BigDecimal getOperatingIncome() { return operatingIncome; }
    public void setOperatingIncome(BigDecimal operatingIncome) { this.operatingIncome = operatingIncome; }
    public BigDecimal getNetIncome() { return netIncome; }
    public void setNetIncome(BigDecimal netIncome) { this.netIncome = netIncome; }
    public BigDecimal getTotalAssets() { return totalAssets; }
    public void setTotalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; }
    public BigDecimal getTotalLiabilities() { return totalLiabilities; }
    public void setTotalLiabilities(BigDecimal totalLiabilities) { this.totalLiabilities = totalLiabilities; }
    public BigDecimal getTotalEquity() { return totalEquity; }
    public void setTotalEquity(BigDecimal totalEquity) { this.totalEquity = totalEquity; }
    public BigDecimal getOperatingCashFlow() { return operatingCashFlow; }
    public void setOperatingCashFlow(BigDecimal operatingCashFlow) { this.operatingCashFlow = operatingCashFlow; }
    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }
}
