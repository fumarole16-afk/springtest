package com.stockanalyzer.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_prices",
       uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "date"}),
       indexes = @Index(columnList = "stock_id, date"))
public class DailyPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private LocalDate date;

    @Column(precision = 12, scale = 4)
    private BigDecimal open;

    @Column(precision = 12, scale = 4)
    private BigDecimal high;

    @Column(precision = 12, scale = 4)
    private BigDecimal low;

    @Column(name = "close_price", precision = 12, scale = 4)
    private BigDecimal close;

    @Column(name = "adjusted_close", precision = 12, scale = 4)
    private BigDecimal adjustedClose;

    private Long volume;

    public DailyPrice() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }
    public BigDecimal getAdjustedClose() { return adjustedClose; }
    public void setAdjustedClose(BigDecimal adjustedClose) { this.adjustedClose = adjustedClose; }
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
}
