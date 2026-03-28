package com.stockanalyzer.entity;

import javax.persistence.*;

@Entity
@Table(name = "stock_news",
        uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "news_id"}))
public class StockNews {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    public StockNews() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }
    public News getNews() { return news; }
    public void setNews(News news) { this.news = news; }
}
