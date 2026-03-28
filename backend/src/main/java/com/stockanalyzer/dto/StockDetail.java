package com.stockanalyzer.dto;

import java.math.BigDecimal;

public class StockDetail {
    private String ticker;
    private String companyName;
    private String exchange;
    private BigDecimal marketCap;
    private BigDecimal currentPrice;
    private BigDecimal trailingPE;
    private String sectorName;
    private String industryName;

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public BigDecimal getTrailingPE() { return trailingPE; }
    public void setTrailingPE(BigDecimal trailingPE) { this.trailingPE = trailingPE; }
    public String getSectorName() { return sectorName; }
    public void setSectorName(String sectorName) { this.sectorName = sectorName; }
    public String getIndustryName() { return industryName; }
    public void setIndustryName(String industryName) { this.industryName = industryName; }
}
