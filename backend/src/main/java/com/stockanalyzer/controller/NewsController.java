package com.stockanalyzer.controller;

import com.stockanalyzer.common.ApiResponse;
import com.stockanalyzer.dto.NewsItem;
import com.stockanalyzer.entity.News;
import com.stockanalyzer.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {
    private final NewsService newsService;

    @Autowired
    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ApiResponse<List<News>> getLatestNews(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(newsService.getLatestNews(page, size));
    }

    @GetMapping("/filings")
    public ApiResponse<List<NewsItem>> getFilings(@RequestParam("ticker") String ticker) {
        return ApiResponse.ok(newsService.getFilings(ticker));
    }
}
