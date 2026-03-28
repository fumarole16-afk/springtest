package com.stockanalyzer.service;

import com.stockanalyzer.client.YahooFinanceClient;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import com.stockanalyzer.dto.StockSearchResult;
import com.stockanalyzer.entity.DailyPrice;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.repository.DailyPriceRepository;
import com.stockanalyzer.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockService {
    private final StockRepository stockRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final YahooFinanceClient yahooClient;

    @Autowired
    public StockService(StockRepository stockRepository,
                        DailyPriceRepository dailyPriceRepository,
                        YahooFinanceClient yahooClient) {
        this.stockRepository = stockRepository;
        this.dailyPriceRepository = dailyPriceRepository;
        this.yahooClient = yahooClient;
    }

    public List<StockSearchResult> search(String keyword) {
        return stockRepository.search(keyword, 10).stream()
                .map(s -> new StockSearchResult(s.getTicker(), s.getCompanyName(), s.getExchange()))
                .collect(Collectors.toList());
    }

    public StockDetail getDetail(String ticker) {
        Stock stock = stockRepository.findByTicker(ticker.toUpperCase());
        if (stock == null) {
            return yahooClient.fetchQuote(ticker);
        }
        StockDetail detail = yahooClient.fetchQuote(ticker);
        detail.setTicker(stock.getTicker());
        detail.setCompanyName(stock.getCompanyName());
        detail.setExchange(stock.getExchange());
        if (stock.getIndustry() != null) {
            detail.setIndustryName(stock.getIndustry().getName());
            detail.setSectorName(stock.getIndustry().getSector().getName());
        }
        return detail;
    }

    public List<PriceData> getPrices(String ticker, String period) {
        Stock stock = stockRepository.findByTicker(ticker.toUpperCase());
        if (stock == null) {
            return yahooClient.fetchPriceHistory(ticker, periodToRange(period), "1d");
        }
        LocalDate to = LocalDate.now();
        LocalDate from = periodToFromDate(period, to);
        return dailyPriceRepository.findByStockAndDateRange(stock.getId(), from, to)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private PriceData toDto(DailyPrice dp) {
        PriceData pd = new PriceData();
        pd.setDate(dp.getDate());
        pd.setOpen(dp.getOpen());
        pd.setHigh(dp.getHigh());
        pd.setLow(dp.getLow());
        pd.setClose(dp.getClose());
        pd.setAdjustedClose(dp.getAdjustedClose());
        pd.setVolume(dp.getVolume());
        return pd;
    }

    private LocalDate periodToFromDate(String period, LocalDate to) {
        switch (period) {
            case "1m": return to.minusMonths(1);
            case "3m": return to.minusMonths(3);
            case "1y": return to.minusYears(1);
            case "5y": return to.minusYears(5);
            default: return to.minusMonths(1);
        }
    }

    private String periodToRange(String period) {
        switch (period) {
            case "1m": return "1mo";
            case "3m": return "3mo";
            case "1y": return "1y";
            case "5y": return "5y";
            default: return "1mo";
        }
    }
}
