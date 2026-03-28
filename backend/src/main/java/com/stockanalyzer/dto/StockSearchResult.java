package com.stockanalyzer.dto;

public class StockSearchResult {
    private String ticker;
    private String companyName;
    private String exchange;

    public StockSearchResult(String ticker, String companyName, String exchange) {
        this.ticker = ticker;
        this.companyName = companyName;
        this.exchange = exchange;
    }

    public String getTicker() { return ticker; }
    public String getCompanyName() { return companyName; }
    public String getExchange() { return exchange; }
}
