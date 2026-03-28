package com.stockanalyzer.dto;

import java.math.BigDecimal;

public class SectorOverview {
    private Long sectorId;
    private String sectorName;
    private BigDecimal avgPer;
    private BigDecimal avgPbr;
    private BigDecimal totalMarketCap;
    private BigDecimal avgDividendYield;

    public Long getSectorId() { return sectorId; }
    public void setSectorId(Long sectorId) { this.sectorId = sectorId; }
    public String getSectorName() { return sectorName; }
    public void setSectorName(String sectorName) { this.sectorName = sectorName; }
    public BigDecimal getAvgPer() { return avgPer; }
    public void setAvgPer(BigDecimal avgPer) { this.avgPer = avgPer; }
    public BigDecimal getAvgPbr() { return avgPbr; }
    public void setAvgPbr(BigDecimal avgPbr) { this.avgPbr = avgPbr; }
    public BigDecimal getTotalMarketCap() { return totalMarketCap; }
    public void setTotalMarketCap(BigDecimal totalMarketCap) { this.totalMarketCap = totalMarketCap; }
    public BigDecimal getAvgDividendYield() { return avgDividendYield; }
    public void setAvgDividendYield(BigDecimal avgDividendYield) { this.avgDividendYield = avgDividendYield; }
}
