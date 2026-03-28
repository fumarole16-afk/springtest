package com.stockanalyzer.dto;

import java.math.BigDecimal;

public class IndexQuote {
    private String symbol;
    private String name;
    private BigDecimal price;
    private BigDecimal change;
    private BigDecimal changePercent;

    public IndexQuote() {}

    public IndexQuote(String symbol, String name, BigDecimal price,
                      BigDecimal change, BigDecimal changePercent) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
        this.change = change;
        this.changePercent = changePercent;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getChange() { return change; }
    public void setChange(BigDecimal change) { this.change = change; }
    public BigDecimal getChangePercent() { return changePercent; }
    public void setChangePercent(BigDecimal changePercent) { this.changePercent = changePercent; }
}
