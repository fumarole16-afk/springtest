package com.stockanalyzer.dto;

import java.math.BigDecimal;

public class ScreeningFilter {
    private Long sectorId;
    private BigDecimal minPer;
    private BigDecimal maxPer;
    private BigDecimal minPbr;
    private BigDecimal maxPbr;
    private BigDecimal minCap;
    private BigDecimal maxCap;
    private BigDecimal minDividendYield;
    private BigDecimal minRoe;
    private BigDecimal maxDebtRatio;
    private BigDecimal minRevenueGrowth;
    private BigDecimal minOperatingMargin;
    private String exchange;

    public Long getSectorId() { return sectorId; }
    public void setSectorId(Long sectorId) { this.sectorId = sectorId; }
    public BigDecimal getMinPer() { return minPer; }
    public void setMinPer(BigDecimal minPer) { this.minPer = minPer; }
    public BigDecimal getMaxPer() { return maxPer; }
    public void setMaxPer(BigDecimal maxPer) { this.maxPer = maxPer; }
    public BigDecimal getMinPbr() { return minPbr; }
    public void setMinPbr(BigDecimal minPbr) { this.minPbr = minPbr; }
    public BigDecimal getMaxPbr() { return maxPbr; }
    public void setMaxPbr(BigDecimal maxPbr) { this.maxPbr = maxPbr; }
    public BigDecimal getMinCap() { return minCap; }
    public void setMinCap(BigDecimal minCap) { this.minCap = minCap; }
    public BigDecimal getMaxCap() { return maxCap; }
    public void setMaxCap(BigDecimal maxCap) { this.maxCap = maxCap; }
    public BigDecimal getMinDividendYield() { return minDividendYield; }
    public void setMinDividendYield(BigDecimal minDividendYield) { this.minDividendYield = minDividendYield; }
    public BigDecimal getMinRoe() { return minRoe; }
    public void setMinRoe(BigDecimal minRoe) { this.minRoe = minRoe; }
    public BigDecimal getMaxDebtRatio() { return maxDebtRatio; }
    public void setMaxDebtRatio(BigDecimal maxDebtRatio) { this.maxDebtRatio = maxDebtRatio; }
    public BigDecimal getMinRevenueGrowth() { return minRevenueGrowth; }
    public void setMinRevenueGrowth(BigDecimal minRevenueGrowth) { this.minRevenueGrowth = minRevenueGrowth; }
    public BigDecimal getMinOperatingMargin() { return minOperatingMargin; }
    public void setMinOperatingMargin(BigDecimal minOperatingMargin) { this.minOperatingMargin = minOperatingMargin; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
}
