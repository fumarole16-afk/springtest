package com.stockanalyzer.scheduler;

import com.stockanalyzer.client.SecEdgarClient;
import com.stockanalyzer.dto.NewsItem;
import com.stockanalyzer.entity.News;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.entity.StockNews;
import com.stockanalyzer.repository.NewsRepository;
import com.stockanalyzer.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class NewsCollector {
    private static final Logger log = LoggerFactory.getLogger(NewsCollector.class);

    private final StockRepository stockRepository;
    private final NewsRepository newsRepository;
    private final SecEdgarClient secEdgarClient;

    @Autowired
    public NewsCollector(StockRepository stockRepository, NewsRepository newsRepository,
                         SecEdgarClient secEdgarClient) {
        this.stockRepository = stockRepository;
        this.newsRepository = newsRepository;
        this.secEdgarClient = secEdgarClient;
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void collectNews() {
        List<Stock> stocks = stockRepository.findAll();
        log.info("Starting news collection (SEC EDGAR filings) for {} stocks", stocks.size());

        for (Stock stock : stocks) {
            try {
                collectEdgarFilings(stock);
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("Failed to collect news for {}: {}", stock.getTicker(), e.getMessage());
            }
        }
        log.info("News collection completed");
    }

    @Transactional
    public void collectEdgarFilings(Stock stock) {
        List<NewsItem> items = secEdgarClient.fetchFilings(stock.getTicker(), 10);
        for (NewsItem item : items) {
            News news = toNewsEntity(item, "SEC EDGAR");
            news = newsRepository.save(news);
            StockNews sn = new StockNews();
            sn.setStock(stock);
            sn.setNews(news);
            try {
                newsRepository.saveStockNews(sn);
            } catch (Exception e) {
                log.debug("Duplicate stock_news skipped for {} / news {}", stock.getTicker(), news.getId());
            }
        }
    }

    private News toNewsEntity(NewsItem item, String source) {
        News news = new News();
        news.setTitle(item.getTitle() != null ? item.getTitle() : "(no title)");
        news.setSource(source);
        news.setUrl(item.getUrl() != null ? item.getUrl() : "");
        news.setPublishedAt(parseDateTime(item.getPublishedAt()));
        news.setSummary(item.getSummary());
        news.setImageUrl(item.getImageUrl());
        return news;
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isEmpty()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(s.replace("Z", "").replace("T", "T"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
