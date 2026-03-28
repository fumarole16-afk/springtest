package com.stockanalyzer.dto;

import java.math.BigDecimal;
import java.util.Map;

public class SectorPerformance {
    private Long sectorId;
    private String sectorName;
    private Map<String, BigDecimal> performances;

    public Long getSectorId() { return sectorId; }
    public void setSectorId(Long sectorId) { this.sectorId = sectorId; }
    public String getSectorName() { return sectorName; }
    public void setSectorName(String sectorName) { this.sectorName = sectorName; }
    public Map<String, BigDecimal> getPerformances() { return performances; }
    public void setPerformances(Map<String, BigDecimal> performances) { this.performances = performances; }
}
