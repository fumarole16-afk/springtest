package com.stockanalyzer.controller;

import com.stockanalyzer.common.ApiResponse;
import com.stockanalyzer.dto.FinancialData;
import com.stockanalyzer.dto.FinancialRatios;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import com.stockanalyzer.dto.StockSearchResult;
import com.stockanalyzer.entity.News;
import com.stockanalyzer.service.FinancialService;
import com.stockanalyzer.service.NewsService;
import com.stockanalyzer.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private final StockService stockService;
    private final FinancialService financialService;
    private final NewsService newsService;

    @Autowired
    public StockController(StockService stockService, FinancialService financialService,
                           NewsService newsService) {
        this.stockService = stockService;
        this.financialService = financialService;
        this.newsService = newsService;
    }

    @GetMapping
    public ApiResponse<List<StockSearchResult>> search(@RequestParam("q") String keyword) {
        return ApiResponse.ok(stockService.search(keyword));
    }

    @GetMapping("/{ticker}")
    public ApiResponse<StockDetail> getDetail(@PathVariable String ticker) {
        StockDetail detail = stockService.getDetail(ticker);
        if (detail == null) {
            return ApiResponse.error("Stock not found: " + ticker);
        }
        return ApiResponse.ok(detail);
    }

    @GetMapping("/{ticker}/prices")
    public ApiResponse<List<PriceData>> getPrices(
            @PathVariable String ticker,
            @RequestParam(value = "period", defaultValue = "1m") String period) {
        return ApiResponse.ok(stockService.getPrices(ticker, period));
    }

    @GetMapping("/{ticker}/financials")
    public ApiResponse<List<FinancialData>> getFinancials(
            @PathVariable String ticker,
            @RequestParam(value = "type", defaultValue = "annual") String type) {
        return ApiResponse.ok(financialService.getFinancials(ticker, type));
    }

    @GetMapping("/{ticker}/financials/ratios")
    public ApiResponse<FinancialRatios> getRatios(@PathVariable String ticker) {
        return ApiResponse.ok(financialService.getRatios(ticker));
    }

    @GetMapping("/{ticker}/news")
    public ApiResponse<List<News>> getNews(
            @PathVariable String ticker,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(newsService.getNewsByTicker(ticker, page, size));
    }
}
