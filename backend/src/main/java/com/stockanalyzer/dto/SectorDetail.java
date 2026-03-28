package com.stockanalyzer.dto;

import java.util.List;

public class SectorDetail {
    private Long sectorId;
    private String sectorName;
    private String description;
    private SectorOverview metric;
    private List<IndustryDetail> industries;

    public Long getSectorId() { return sectorId; }
    public void setSectorId(Long sectorId) { this.sectorId = sectorId; }
    public String getSectorName() { return sectorName; }
    public void setSectorName(String sectorName) { this.sectorName = sectorName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public SectorOverview getMetric() { return metric; }
    public void setMetric(SectorOverview metric) { this.metric = metric; }
    public List<IndustryDetail> getIndustries() { return industries; }
    public void setIndustries(List<IndustryDetail> industries) { this.industries = industries; }
}
