package com.stockanalyzer.dto;

import java.math.BigDecimal;

public class MoverStock {
    private String ticker;
    private String companyName;
    private BigDecimal price;
    private BigDecimal changePercent;

    public MoverStock() {}

    public MoverStock(String ticker, String companyName, BigDecimal price, BigDecimal changePercent) {
        this.ticker = ticker;
        this.companyName = companyName;
        this.price = price;
        this.changePercent = changePercent;
    }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getChangePercent() { return changePercent; }
    public void setChangePercent(BigDecimal changePercent) { this.changePercent = changePercent; }
}
