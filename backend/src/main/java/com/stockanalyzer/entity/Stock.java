package com.stockanalyzer.entity;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "stocks")
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String ticker;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(length = 20)
    private String exchange;

    @Column(name = "market_cap", precision = 20, scale = 2)
    private BigDecimal marketCap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_id")
    private Industry industry;

    public Stock() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
    public Industry getIndustry() { return industry; }
    public void setIndustry(Industry industry) { this.industry = industry; }
}
