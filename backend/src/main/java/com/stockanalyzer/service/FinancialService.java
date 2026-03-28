package com.stockanalyzer.service;

import com.stockanalyzer.dto.FinancialData;
import com.stockanalyzer.dto.FinancialRatios;
import com.stockanalyzer.entity.Financial;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.repository.FinancialRepository;
import com.stockanalyzer.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinancialService {

    private final FinancialRepository financialRepository;
    private final StockRepository stockRepository;

    @Autowired
    public FinancialService(FinancialRepository financialRepository, StockRepository stockRepository) {
        this.financialRepository = financialRepository;
        this.stockRepository = stockRepository;
    }

    public List<FinancialData> getFinancials(String ticker, String type) {
        Stock stock = stockRepository.findByTicker(ticker.toUpperCase());
        if (stock == null) return new ArrayList<>();
        return financialRepository.findByStockAndType(stock.getId(), type)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public FinancialRatios getRatios(String ticker) {
        Stock stock = stockRepository.findByTicker(ticker.toUpperCase());
        if (stock == null) return new FinancialRatios(new HashMap<>(), new HashMap<>());

        Financial latest = financialRepository.findLatestByStock(stock.getId(), "annual");
        if (latest == null) return new FinancialRatios(new HashMap<>(), new HashMap<>());

        Map<String, BigDecimal> stockRatios = calculateRatios(latest);

        // Sector average — to be populated by MetricsCalculator in a later phase
        Map<String, BigDecimal> sectorAvg = new HashMap<>();

        return new FinancialRatios(stockRatios, sectorAvg);
    }

    private Map<String, BigDecimal> calculateRatios(Financial f) {
        Map<String, BigDecimal> ratios = new HashMap<>();
        BigDecimal equity = f.getTotalEquity();
        BigDecimal assets = f.getTotalAssets();
        BigDecimal revenue = f.getRevenue();
        BigDecimal liabilities = f.getTotalLiabilities();

        if (equity != null && equity.compareTo(BigDecimal.ZERO) != 0) {
            ratios.put("roe", f.getNetIncome().divide(equity, 4, RoundingMode.HALF_UP));
            ratios.put("debtRatio", liabilities.divide(equity, 4, RoundingMode.HALF_UP));
        }
        if (assets != null && assets.compareTo(BigDecimal.ZERO) != 0) {
            ratios.put("roa", f.getNetIncome().divide(assets, 4, RoundingMode.HALF_UP));
        }
        if (revenue != null && revenue.compareTo(BigDecimal.ZERO) != 0) {
            ratios.put("operatingMargin", f.getOperatingIncome().divide(revenue, 4, RoundingMode.HALF_UP));
            ratios.put("netMargin", f.getNetIncome().divide(revenue, 4, RoundingMode.HALF_UP));
        }

        return ratios;
    }

    private FinancialData toDto(Financial f) {
        FinancialData d = new FinancialData();
        d.setPeriod(f.getPeriod());
        d.setType(f.getType());
        d.setRevenue(f.getRevenue());
        d.setOperatingIncome(f.getOperatingIncome());
        d.setNetIncome(f.getNetIncome());
        d.setTotalAssets(f.getTotalAssets());
        d.setTotalLiabilities(f.getTotalLiabilities());
        d.setTotalEquity(f.getTotalEquity());
        d.setOperatingCashFlow(f.getOperatingCashFlow());
        return d;
    }
}
