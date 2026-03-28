package com.stockanalyzer.service;

import com.stockanalyzer.dto.IndustryDetail;
import com.stockanalyzer.dto.SectorDetail;
import com.stockanalyzer.dto.SectorOverview;
import com.stockanalyzer.entity.Industry;
import com.stockanalyzer.entity.IndustryMetric;
import com.stockanalyzer.entity.Sector;
import com.stockanalyzer.entity.SectorMetric;
import com.stockanalyzer.repository.IndustryMetricRepository;
import com.stockanalyzer.repository.IndustryRepository;
import com.stockanalyzer.repository.SectorMetricRepository;
import com.stockanalyzer.repository.SectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SectorService {

    private final SectorRepository sectorRepository;
    private final SectorMetricRepository sectorMetricRepository;
    private final IndustryRepository industryRepository;
    private final IndustryMetricRepository industryMetricRepository;

    @Autowired
    public SectorService(SectorRepository sectorRepository,
                         SectorMetricRepository sectorMetricRepository,
                         IndustryRepository industryRepository,
                         IndustryMetricRepository industryMetricRepository) {
        this.sectorRepository = sectorRepository;
        this.sectorMetricRepository = sectorMetricRepository;
        this.industryRepository = industryRepository;
        this.industryMetricRepository = industryMetricRepository;
    }

    public List<SectorOverview> getAllSectors() {
        return sectorMetricRepository.findLatestAll()
                .stream().map(this::toOverview).collect(Collectors.toList());
    }

    public SectorDetail getSectorDetail(Long sectorId) {
        Sector sector = sectorRepository.findById(sectorId);
        if (sector == null) return null;

        SectorMetric metric = sectorMetricRepository.findLatestBySector(sectorId);

        List<IndustryMetric> industryMetrics = industryMetricRepository.findLatestBySector(sectorId);
        List<IndustryDetail> industries = industryMetrics.stream()
                .map(this::toIndustryDetail).collect(Collectors.toList());

        SectorDetail detail = new SectorDetail();
        detail.setSectorId(sector.getId());
        detail.setSectorName(sector.getName());
        detail.setDescription(sector.getDescription());
        detail.setMetric(metric != null ? toOverview(metric) : null);
        detail.setIndustries(industries);
        return detail;
    }

    public List<SectorOverview> getSectorRankings(Long sectorId, String sort) {
        // Returns all sectors ranked; sort parameter reserved for future use
        return getAllSectors();
    }

    public IndustryDetail getIndustryDetail(Long industryId) {
        IndustryMetric metric = industryMetricRepository.findLatestByIndustry(industryId);
        if (metric == null) {
            Industry industry = industryRepository.findById(industryId);
            if (industry == null) return null;
            IndustryDetail detail = new IndustryDetail();
            detail.setIndustryId(industry.getId());
            detail.setIndustryName(industry.getName());
            detail.setPerformances(new HashMap<>());
            return detail;
        }
        return toIndustryDetail(metric);
    }

    private SectorOverview toOverview(SectorMetric sm) {
        SectorOverview o = new SectorOverview();
        o.setSectorId(sm.getSector().getId());
        o.setSectorName(sm.getSector().getName());
        o.setAvgPer(sm.getAvgPer());
        o.setAvgPbr(sm.getAvgPbr());
        o.setTotalMarketCap(sm.getTotalMarketCap());
        o.setAvgDividendYield(sm.getAvgDividendYield());
        return o;
    }

    private IndustryDetail toIndustryDetail(IndustryMetric im) {
        IndustryDetail d = new IndustryDetail();
        d.setIndustryId(im.getIndustry().getId());
        d.setIndustryName(im.getIndustry().getName());
        d.setAvgPer(im.getAvgPer());
        d.setAvgPbr(im.getAvgPbr());
        d.setTotalMarketCap(im.getTotalMarketCap());
        d.setStockCount(im.getStockCount());

        Map<String, BigDecimal> performances = new HashMap<>();
        if (im.getPerformance1d() != null) performances.put("1d", im.getPerformance1d());
        if (im.getPerformance1w() != null) performances.put("1w", im.getPerformance1w());
        if (im.getPerformance1m() != null) performances.put("1m", im.getPerformance1m());
        if (im.getPerformance3m() != null) performances.put("3m", im.getPerformance3m());
        if (im.getPerformance1y() != null) performances.put("1y", im.getPerformance1y());
        d.setPerformances(performances);
        return d;
    }
}
