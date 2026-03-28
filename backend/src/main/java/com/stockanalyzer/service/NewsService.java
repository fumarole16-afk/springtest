package com.stockanalyzer.service;

import com.stockanalyzer.client.SecEdgarClient;
import com.stockanalyzer.dto.NewsItem;
import com.stockanalyzer.entity.News;
import com.stockanalyzer.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsService {
    private final NewsRepository newsRepository;
    private final SecEdgarClient secEdgarClient;

    @Autowired
    public NewsService(NewsRepository newsRepository, SecEdgarClient secEdgarClient) {
        this.newsRepository = newsRepository;
        this.secEdgarClient = secEdgarClient;
    }

    public List<News> getNewsByTicker(String ticker, int page, int size) {
        return newsRepository.findByStockTicker(ticker, page, size);
    }

    public List<News> getLatestNews(int page, int size) {
        return newsRepository.findLatest(page, size);
    }

    public List<NewsItem> getFilings(String ticker) {
        return secEdgarClient.fetchFilings(ticker, 20);
    }
}
