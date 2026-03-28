package com.stockanalyzer.service;

import com.stockanalyzer.dto.ScreeningFilter;
import com.stockanalyzer.dto.ScreeningResult;
import com.stockanalyzer.entity.StockMetric;
import com.stockanalyzer.repository.StockMetricRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScreeningService {

    private final StockMetricRepository stockMetricRepository;

    @Autowired
    public ScreeningService(StockMetricRepository stockMetricRepository) {
        this.stockMetricRepository = stockMetricRepository;
    }

    public List<ScreeningResult> screen(ScreeningFilter filter, int page, int size, String sort) {
        return stockMetricRepository.findByFilters(filter, page, size, sort)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public long count(ScreeningFilter filter) {
        return stockMetricRepository.countByFilters(filter);
    }

    private ScreeningResult toDto(StockMetric m) {
        ScreeningResult r = new ScreeningResult();
        r.setTicker(m.getStock().getTicker());
        r.setCompanyName(m.getStock().getCompanyName());
        r.setExchange(m.getStock().getExchange());
        r.setMarketCap(m.getStock().getMarketCap());
        r.setPer(m.getPer());
        r.setPbr(m.getPbr());
        r.setRoe(m.getRoe());
        r.setDebtRatio(m.getDebtRatio());
        r.setDividendYield(m.getDividendYield());
        r.setRevenueGrowth(m.getRevenueGrowth());
        r.setOperatingMargin(m.getOperatingMargin());
        return r;
    }
}
