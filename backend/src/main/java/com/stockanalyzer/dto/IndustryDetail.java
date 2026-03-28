package com.stockanalyzer.dto;

import java.math.BigDecimal;
import java.util.Map;

public class IndustryDetail {
    private Long industryId;
    private String industryName;
    private BigDecimal avgPer;
    private BigDecimal avgPbr;
    private BigDecimal totalMarketCap;
    private Integer stockCount;
    private Map<String, BigDecimal> performances;

    public Long getIndustryId() { return industryId; }
    public void setIndustryId(Long industryId) { this.industryId = industryId; }
    public String getIndustryName() { return industryName; }
    public void setIndustryName(String industryName) { this.industryName = industryName; }
    public BigDecimal getAvgPer() { return avgPer; }
    public void setAvgPer(BigDecimal avgPer) { this.avgPer = avgPer; }
    public BigDecimal getAvgPbr() { return avgPbr; }
    public void setAvgPbr(BigDecimal avgPbr) { this.avgPbr = avgPbr; }
    public BigDecimal getTotalMarketCap() { return totalMarketCap; }
    public void setTotalMarketCap(BigDecimal totalMarketCap) { this.totalMarketCap = totalMarketCap; }
    public Integer getStockCount() { return stockCount; }
    public void setStockCount(Integer stockCount) { this.stockCount = stockCount; }
    public Map<String, BigDecimal> getPerformances() { return performances; }
    public void setPerformances(Map<String, BigDecimal> performances) { this.performances = performances; }
}
