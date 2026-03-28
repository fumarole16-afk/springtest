package com.stockanalyzer.dto;

import java.math.BigDecimal;
import java.util.Map;

public class FinancialRatios {
    private Map<String, BigDecimal> stock;
    private Map<String, BigDecimal> sectorAverage;

    public FinancialRatios(Map<String, BigDecimal> stock, Map<String, BigDecimal> sectorAverage) {
        this.stock = stock;
        this.sectorAverage = sectorAverage;
    }

    public Map<String, BigDecimal> getStock() { return stock; }
    public Map<String, BigDecimal> getSectorAverage() { return sectorAverage; }
}
