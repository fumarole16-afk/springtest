package com.stockanalyzer.scheduler;

import com.stockanalyzer.client.YahooFinanceClient;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import com.stockanalyzer.entity.DailyPrice;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.repository.DailyPriceRepository;
import com.stockanalyzer.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PriceCollector {
    private static final Logger log = LoggerFactory.getLogger(PriceCollector.class);

    private static final List<String> INITIAL_TICKERS = Arrays.asList(
            "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "TSLA", "BRK-B",
            "JPM", "JNJ", "V", "UNH", "HD", "PG", "MA", "XOM", "BAC", "ABBV",
            "KO", "PFE", "MRK", "PEP", "COST", "TMO", "AVGO", "CSCO", "ACN",
            "MCD", "ABT", "WMT", "NKE", "DIS", "ADBE", "CRM", "NFLX", "AMD",
            "INTC", "QCOM", "TXN", "ORCL"
    );

    private final YahooFinanceClient yahooClient;
    private final StockRepository stockRepository;
    private final DailyPriceRepository dailyPriceRepository;

    @Autowired
    public PriceCollector(YahooFinanceClient yahooClient,
                          StockRepository stockRepository,
                          DailyPriceRepository dailyPriceRepository) {
        this.yahooClient = yahooClient;
        this.stockRepository = stockRepository;
        this.dailyPriceRepository = dailyPriceRepository;
    }

    @Scheduled(cron = "0 0 7 ? * TUE-SAT")
    public void collectDailyPrices() {
        log.info("Starting daily price collection for {} tickers", INITIAL_TICKERS.size());
        for (String ticker : INITIAL_TICKERS) {
            try {
                collectForTicker(ticker);
            } catch (Exception e) {
                log.error("Failed to collect data for {}: {}", ticker, e.getMessage());
            }
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        }
        log.info("Daily price collection completed");
    }

    @Transactional
    public void collectForTicker(String ticker) {
        Stock stock = stockRepository.findByTicker(ticker);
        if (stock == null) {
            StockDetail detail = yahooClient.fetchQuote(ticker);
            stock = new Stock();
            stock.setTicker(detail.getTicker());
            stock.setCompanyName(detail.getCompanyName());
            stock.setExchange(detail.getExchange());
            stock.setMarketCap(detail.getMarketCap());
            stock = stockRepository.save(stock);
        }
        List<PriceData> prices = yahooClient.fetchPriceHistory(ticker, "1mo", "1d");
        Stock finalStock = stock;
        List<DailyPrice> entities = prices.stream().map(pd -> {
            DailyPrice dp = new DailyPrice();
            dp.setStock(finalStock);
            dp.setDate(pd.getDate());
            dp.setOpen(pd.getOpen());
            dp.setHigh(pd.getHigh());
            dp.setLow(pd.getLow());
            dp.setClose(pd.getClose());
            dp.setAdjustedClose(pd.getAdjustedClose());
            dp.setVolume(pd.getVolume());
            return dp;
        }).collect(Collectors.toList());
        dailyPriceRepository.saveAll(entities);
        log.info("Collected {} price records for {}", entities.size(), ticker);
    }
}
