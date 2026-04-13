package com.stockanalyzer.scheduler;

import com.stockanalyzer.client.SecEdgarClient;
import com.stockanalyzer.client.YahooFinanceClient;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import com.stockanalyzer.entity.DailyPrice;
import com.stockanalyzer.entity.Industry;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.repository.DailyPriceRepository;
import com.stockanalyzer.repository.IndustryRepository;
import com.stockanalyzer.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static final Map<String, String> TICKER_TO_INDUSTRY = new HashMap<>();
    static {
        TICKER_TO_INDUSTRY.put("AAPL", "Hardware");
        TICKER_TO_INDUSTRY.put("MSFT", "Software");
        TICKER_TO_INDUSTRY.put("GOOGL", "Media");
        TICKER_TO_INDUSTRY.put("AMZN", "Retail");
        TICKER_TO_INDUSTRY.put("NVDA", "Semiconductors");
        TICKER_TO_INDUSTRY.put("META", "Media");
        TICKER_TO_INDUSTRY.put("TSLA", "Automobiles");
        TICKER_TO_INDUSTRY.put("BRK-B", "Insurance");
        TICKER_TO_INDUSTRY.put("JPM", "Banks");
        TICKER_TO_INDUSTRY.put("JNJ", "Pharmaceuticals");
        TICKER_TO_INDUSTRY.put("V", "Capital Markets");
        TICKER_TO_INDUSTRY.put("UNH", "Medical Devices");
        TICKER_TO_INDUSTRY.put("HD", "Retail");
        TICKER_TO_INDUSTRY.put("PG", "Household Products");
        TICKER_TO_INDUSTRY.put("MA", "Capital Markets");
        TICKER_TO_INDUSTRY.put("XOM", "Oil & Gas");
        TICKER_TO_INDUSTRY.put("BAC", "Banks");
        TICKER_TO_INDUSTRY.put("ABBV", "Pharmaceuticals");
        TICKER_TO_INDUSTRY.put("KO", "Food & Beverage");
        TICKER_TO_INDUSTRY.put("PFE", "Pharmaceuticals");
        TICKER_TO_INDUSTRY.put("MRK", "Pharmaceuticals");
        TICKER_TO_INDUSTRY.put("PEP", "Food & Beverage");
        TICKER_TO_INDUSTRY.put("COST", "Retail");
        TICKER_TO_INDUSTRY.put("TMO", "Medical Devices");
        TICKER_TO_INDUSTRY.put("AVGO", "Semiconductors");
        TICKER_TO_INDUSTRY.put("CSCO", "Hardware");
        TICKER_TO_INDUSTRY.put("ACN", "Software");
        TICKER_TO_INDUSTRY.put("MCD", "Retail");
        TICKER_TO_INDUSTRY.put("ABT", "Medical Devices");
        TICKER_TO_INDUSTRY.put("WMT", "Retail");
        TICKER_TO_INDUSTRY.put("NKE", "Retail");
        TICKER_TO_INDUSTRY.put("DIS", "Media");
        TICKER_TO_INDUSTRY.put("ADBE", "Software");
        TICKER_TO_INDUSTRY.put("CRM", "Software");
        TICKER_TO_INDUSTRY.put("NFLX", "Media");
        TICKER_TO_INDUSTRY.put("AMD", "Semiconductors");
        TICKER_TO_INDUSTRY.put("INTC", "Semiconductors");
        TICKER_TO_INDUSTRY.put("QCOM", "Semiconductors");
        TICKER_TO_INDUSTRY.put("TXN", "Semiconductors");
        TICKER_TO_INDUSTRY.put("ORCL", "Software");
    }

    private final YahooFinanceClient yahooClient;
    private final StockRepository stockRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final SecEdgarClient secEdgarClient;
    private final IndustryRepository industryRepository;

    @Autowired
    public PriceCollector(YahooFinanceClient yahooClient,
                          StockRepository stockRepository,
                          DailyPriceRepository dailyPriceRepository,
                          SecEdgarClient secEdgarClient,
                          IndustryRepository industryRepository) {
        this.yahooClient = yahooClient;
        this.stockRepository = stockRepository;
        this.dailyPriceRepository = dailyPriceRepository;
        this.secEdgarClient = secEdgarClient;
        this.industryRepository = industryRepository;
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
            stock = stockRepository.save(stock);
        }

        // Assign industry if missing (one-time backfill using ticker→industry map)
        if (stock.getIndustry() == null) {
            String industryName = TICKER_TO_INDUSTRY.get(ticker);
            if (industryName != null) {
                Industry industry = industryRepository.findByName(industryName);
                if (industry != null) {
                    stock.setIndustry(industry);
                    stock = stockRepository.save(stock);
                }
            }
        }

        // Update marketCap: currentPrice * sharesOutstanding
        try {
            StockDetail quote = yahooClient.fetchQuote(ticker);
            BigDecimal currentPrice = quote.getCurrentPrice();
            if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                String factsJson = secEdgarClient.fetchCompanyFacts(ticker);
                if (factsJson != null) {
                    BigDecimal shares = SecEdgarClient.parseSharesOutstanding(factsJson);
                    if (shares != null) {
                        stock.setMarketCap(currentPrice.multiply(shares));
                        stockRepository.save(stock);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update marketCap for {}: {}", ticker, e.getMessage());
        }

        List<PriceData> prices = yahooClient.fetchPriceHistory(ticker, "1mo", "1d");
        if (prices.isEmpty()) {
            log.info("No price data returned for {}", ticker);
            return;
        }

        // Pre-filter: skip dates already in DB to avoid (stock_id, date) unique violation.
        // saveAll() is NOT upsert — a single conflict rolls back the entire batch.
        LocalDate minDate = prices.stream().map(PriceData::getDate).min(LocalDate::compareTo).orElse(null);
        LocalDate maxDate = prices.stream().map(PriceData::getDate).max(LocalDate::compareTo).orElse(null);
        Set<LocalDate> existingDates = dailyPriceRepository
                .findByStockAndDateRange(stock.getId(), minDate, maxDate)
                .stream().map(DailyPrice::getDate).collect(Collectors.toSet());

        Stock finalStock = stock;
        List<DailyPrice> entities = prices.stream()
                .filter(pd -> !existingDates.contains(pd.getDate()))
                .map(pd -> {
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

        if (!entities.isEmpty()) {
            dailyPriceRepository.saveAll(entities);
        }
        int skipped = prices.size() - entities.size();
        log.info("Collected {} new price records for {} (skipped {} existing)", entities.size(), ticker, skipped);
    }
}
