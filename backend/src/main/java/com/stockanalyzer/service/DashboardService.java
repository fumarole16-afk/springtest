package com.stockanalyzer.service;

import com.stockanalyzer.client.YahooFinanceClient;
import com.stockanalyzer.dto.IndexQuote;
import com.stockanalyzer.dto.MoverStock;
import com.stockanalyzer.entity.DailyPrice;
import com.stockanalyzer.entity.StockMetric;
import com.stockanalyzer.repository.DailyPriceRepository;
import com.stockanalyzer.repository.StockMetricRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final String[] INDEX_SYMBOLS = {"^GSPC", "^IXIC", "^DJI"};
    private static final String[] INDEX_NAMES = {"S&P 500", "NASDAQ", "Dow Jones"};

    private final YahooFinanceClient yahooClient;
    private final DailyPriceRepository dailyPriceRepository;
    private final StockMetricRepository stockMetricRepository;

    @Autowired
    public DashboardService(YahooFinanceClient yahooClient,
                            DailyPriceRepository dailyPriceRepository,
                            StockMetricRepository stockMetricRepository) {
        this.yahooClient = yahooClient;
        this.dailyPriceRepository = dailyPriceRepository;
        this.stockMetricRepository = stockMetricRepository;
    }

    public List<IndexQuote> getIndices() {
        List<IndexQuote> quotes = new ArrayList<>();
        for (int i = 0; i < INDEX_SYMBOLS.length; i++) {
            quotes.add(yahooClient.fetchIndexQuote(INDEX_SYMBOLS[i], INDEX_NAMES[i]));
        }
        return quotes;
    }

    /**
     * Returns a map with "gainers" and "losers" keys, each containing top 5 MoverStock entries
     * computed from the latest two daily price entries per stock.
     */
    public Map<String, List<MoverStock>> getMovers() {
        LocalDate latestDate = dailyPriceRepository.getLatestDate();
        List<DailyPrice> latestPrices = dailyPriceRepository.findByDate(latestDate);

        // Build a map of stockId -> latest close
        Map<Long, DailyPrice> latestByStock = new HashMap<>();
        for (DailyPrice dp : latestPrices) {
            latestByStock.put(dp.getStock().getId(), dp);
        }

        // For each stock with a latest price, get the previous price to compute change%
        List<MoverStock> movers = new ArrayList<>();
        for (DailyPrice latest : latestPrices) {
            List<DailyPrice> twoRecent = dailyPriceRepository.findLatestTwoByStock(latest.getStock().getId());
            if (twoRecent.size() < 2) continue;
            BigDecimal latestClose = twoRecent.get(0).getClose();
            BigDecimal prevClose = twoRecent.get(1).getClose();
            if (prevClose == null || prevClose.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal changePct = latestClose.subtract(prevClose)
                    .divide(prevClose, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            movers.add(new MoverStock(
                    latest.getStock().getTicker(),
                    latest.getStock().getCompanyName(),
                    latestClose,
                    changePct
            ));
        }

        movers.sort(Comparator.comparing(MoverStock::getChangePercent).reversed());

        List<MoverStock> gainers = movers.stream().limit(5).collect(Collectors.toList());
        List<MoverStock> losers = movers.stream()
                .sorted(Comparator.comparing(MoverStock::getChangePercent))
                .limit(5)
                .collect(Collectors.toList());

        Map<String, List<MoverStock>> result = new LinkedHashMap<>();
        result.put("gainers", gainers);
        result.put("losers", losers);
        return result;
    }

    /**
     * Returns stocks where current price is within 2% of their 52-week high or low.
     */
    public Map<String, List<MoverStock>> getExtremes() {
        List<StockMetric> metrics = stockMetricRepository.findLatestWithExtremes();
        LocalDate latestDate = dailyPriceRepository.getLatestDate();

        // Get latest prices keyed by stockId
        Map<Long, BigDecimal> latestCloseByStock = new HashMap<>();
        for (DailyPrice dp : dailyPriceRepository.findByDate(latestDate)) {
            latestCloseByStock.put(dp.getStock().getId(), dp.getClose());
        }

        List<MoverStock> nearHigh = new ArrayList<>();
        List<MoverStock> nearLow = new ArrayList<>();

        for (StockMetric sm : metrics) {
            BigDecimal currentPrice = latestCloseByStock.get(sm.getStock().getId());
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal week52High = sm.getWeek52High();
            BigDecimal week52Low = sm.getWeek52Low();

            if (week52High != null && week52High.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pctFromHigh = week52High.subtract(currentPrice)
                        .divide(week52High, 6, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).abs();
                if (pctFromHigh.compareTo(new BigDecimal("2")) <= 0) {
                    nearHigh.add(new MoverStock(
                            sm.getStock().getTicker(),
                            sm.getStock().getCompanyName(),
                            currentPrice,
                            pctFromHigh.negate()
                    ));
                }
            }

            if (week52Low != null && week52Low.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pctFromLow = currentPrice.subtract(week52Low)
                        .divide(week52Low, 6, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).abs();
                if (pctFromLow.compareTo(new BigDecimal("2")) <= 0) {
                    nearLow.add(new MoverStock(
                            sm.getStock().getTicker(),
                            sm.getStock().getCompanyName(),
                            currentPrice,
                            pctFromLow
                    ));
                }
            }
        }

        Map<String, List<MoverStock>> result = new LinkedHashMap<>();
        result.put("nearHigh", nearHigh);
        result.put("nearLow", nearLow);
        return result;
    }

    /**
     * Returns stocks where latest volume > 2x avg_volume_30d.
     */
    public List<MoverStock> getVolumeSpikes() {
        List<StockMetric> metricsWithAvg = stockMetricRepository.findLatestWithAvgVolume();
        LocalDate latestDate = dailyPriceRepository.getLatestDate();

        Map<Long, Long> avgVolumeByStock = new HashMap<>();
        for (StockMetric sm : metricsWithAvg) {
            if (sm.getAvgVolume30d() != null) {
                avgVolumeByStock.put(sm.getStock().getId(), sm.getAvgVolume30d());
            }
        }

        List<DailyPrice> latestPrices = dailyPriceRepository.findByDate(latestDate);
        List<MoverStock> spikes = new ArrayList<>();

        for (DailyPrice dp : latestPrices) {
            Long avgVol = avgVolumeByStock.get(dp.getStock().getId());
            if (avgVol == null || avgVol == 0) continue;
            Long currentVol = dp.getVolume();
            if (currentVol == null) continue;
            if (currentVol > 2L * avgVol) {
                BigDecimal ratio = new BigDecimal(currentVol)
                        .divide(new BigDecimal(avgVol), 2, RoundingMode.HALF_UP);
                spikes.add(new MoverStock(
                        dp.getStock().getTicker(),
                        dp.getStock().getCompanyName(),
                        dp.getClose(),
                        ratio
                ));
            }
        }

        spikes.sort(Comparator.comparing(MoverStock::getChangePercent).reversed());
        return spikes;
    }
}
