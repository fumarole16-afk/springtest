package com.stockanalyzer.dto;

import java.math.BigDecimal;

public class ScreeningResult {
    private String ticker;
    private String companyName;
    private String exchange;
    private BigDecimal marketCap;
    private BigDecimal per;
    private BigDecimal pbr;
    private BigDecimal roe;
    private BigDecimal debtRatio;
    private BigDecimal dividendYield;
    private BigDecimal revenueGrowth;
    private BigDecimal operatingMargin;

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
    public BigDecimal getPer() { return per; }
    public void setPer(BigDecimal per) { this.per = per; }
    public BigDecimal getPbr() { return pbr; }
    public void setPbr(BigDecimal pbr) { this.pbr = pbr; }
    public BigDecimal getRoe() { return roe; }
    public void setRoe(BigDecimal roe) { this.roe = roe; }
    public BigDecimal getDebtRatio() { return debtRatio; }
    public void setDebtRatio(BigDecimal debtRatio) { this.debtRatio = debtRatio; }
    public BigDecimal getDividendYield() { return dividendYield; }
    public void setDividendYield(BigDecimal dividendYield) { this.dividendYield = dividendYield; }
    public BigDecimal getRevenueGrowth() { return revenueGrowth; }
    public void setRevenueGrowth(BigDecimal revenueGrowth) { this.revenueGrowth = revenueGrowth; }
    public BigDecimal getOperatingMargin() { return operatingMargin; }
    public void setOperatingMargin(BigDecimal operatingMargin) { this.operatingMargin = operatingMargin; }
}
