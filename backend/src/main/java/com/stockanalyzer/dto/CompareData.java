package com.stockanalyzer.dto;

import java.util.List;
import java.util.Map;

public class CompareData {
    private List<StockDetail> stocks;
    private Map<String, List<PriceData>> prices;
    private Map<String, List<FinancialData>> financials;

    public CompareData(List<StockDetail> stocks,
                       Map<String, List<PriceData>> prices,
                       Map<String, List<FinancialData>> financials) {
        this.stocks = stocks;
        this.prices = prices;
        this.financials = financials;
    }

    public List<StockDetail> getStocks() { return stocks; }
    public void setStocks(List<StockDetail> stocks) { this.stocks = stocks; }
    public Map<String, List<PriceData>> getPrices() { return prices; }
    public void setPrices(Map<String, List<PriceData>> prices) { this.prices = prices; }
    public Map<String, List<FinancialData>> getFinancials() { return financials; }
    public void setFinancials(Map<String, List<FinancialData>> financials) { this.financials = financials; }
}
