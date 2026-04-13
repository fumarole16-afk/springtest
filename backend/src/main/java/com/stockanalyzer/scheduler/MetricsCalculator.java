package com.stockanalyzer.scheduler;

import com.stockanalyzer.entity.*;
import com.stockanalyzer.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Component
public class MetricsCalculator {
    private static final Logger log = LoggerFactory.getLogger(MetricsCalculator.class);

    private final StockRepository stockRepository;
    private final StockMetricRepository stockMetricRepository;
    private final FinancialRepository financialRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final SectorRepository sectorRepository;
    private final SectorMetricRepository sectorMetricRepository;
    private final IndustryRepository industryRepository;
    private final IndustryMetricRepository industryMetricRepository;

    @Autowired
    public MetricsCalculator(StockRepository stockRepository,
                             StockMetricRepository stockMetricRepository,
                             FinancialRepository financialRepository,
                             DailyPriceRepository dailyPriceRepository,
                             SectorRepository sectorRepository,
                             SectorMetricRepository sectorMetricRepository,
                             IndustryRepository industryRepository,
                             IndustryMetricRepository industryMetricRepository) {
        this.stockRepository = stockRepository;
        this.stockMetricRepository = stockMetricRepository;
        this.financialRepository = financialRepository;
        this.dailyPriceRepository = dailyPriceRepository;
        this.sectorRepository = sectorRepository;
        this.sectorMetricRepository = sectorMetricRepository;
        this.industryRepository = industryRepository;
        this.industryMetricRepository = industryMetricRepository;
    }

    @Scheduled(cron = "0 30 7 ? * TUE-SAT")
    public void run() {
        log.info("Starting metrics calculation");
        calculateStockMetrics();
        calculateSectorMetrics();
        calculateIndustryMetrics();
        log.info("Metrics calculation completed");
    }

    @Transactional
    public void calculateStockMetrics() {
        List<Stock> stocks = stockRepository.findAll();
        LocalDate today = LocalDate.now();
        LocalDate yearAgo = today.minusYears(1);
        LocalDate thirtyDaysAgo = today.minusDays(30);

        // Upsert: clear today's rows first to allow safe re-runs
        int deleted = stockMetricRepository.deleteByDate(today);
        if (deleted > 0) {
            log.info("Cleared {} existing stock_metrics rows for {}", deleted, today);
        }

        for (Stock stock : stocks) {
            try {
                StockMetric metric = new StockMetric();
                metric.setStock(stock);
                metric.setDate(today);

                // 52-week high/low and 30d avg volume from daily prices
                List<DailyPrice> yearPrices = dailyPriceRepository.findByStockAndDateRange(stock.getId(), yearAgo, today);
                if (!yearPrices.isEmpty()) {
                    BigDecimal high52 = yearPrices.stream()
                            .map(DailyPrice::getHigh)
                            .filter(v -> v != null)
                            .max(BigDecimal::compareTo).orElse(null);
                    BigDecimal low52 = yearPrices.stream()
                            .map(DailyPrice::getLow)
                            .filter(v -> v != null)
                            .min(BigDecimal::compareTo).orElse(null);
                    metric.setWeek52High(high52);
                    metric.setWeek52Low(low52);

                    List<DailyPrice> month30 = yearPrices.stream()
                            .filter(dp -> !dp.getDate().isBefore(thirtyDaysAgo))
                            .collect(Collectors.toList());
                    if (!month30.isEmpty()) {
                        long totalVolume = month30.stream()
                                .filter(dp -> dp.getVolume() != null)
                                .mapToLong(DailyPrice::getVolume).sum();
                        metric.setAvgVolume30d(totalVolume / month30.size());
                    }
                }

                // Financials-based ratios
                Financial latest = financialRepository.findLatestByStock(stock.getId(), "annual");
                List<Financial> twoLatest = financialRepository.findTwoLatestAnnual(stock.getId());

                if (latest != null) {
                    BigDecimal equity = latest.getTotalEquity();
                    BigDecimal revenue = latest.getRevenue();
                    BigDecimal liabilities = latest.getTotalLiabilities();
                    BigDecimal operatingIncome = latest.getOperatingIncome();
                    BigDecimal netIncome = latest.getNetIncome();

                    if (equity != null && equity.compareTo(BigDecimal.ZERO) != 0 && netIncome != null) {
                        metric.setRoe(netIncome.divide(equity, 4, RoundingMode.HALF_UP));
                    }
                    if (equity != null && equity.compareTo(BigDecimal.ZERO) != 0 && liabilities != null) {
                        metric.setDebtRatio(liabilities.divide(equity, 4, RoundingMode.HALF_UP));
                    }
                    if (revenue != null && revenue.compareTo(BigDecimal.ZERO) != 0 && operatingIncome != null) {
                        metric.setOperatingMargin(operatingIncome.divide(revenue, 4, RoundingMode.HALF_UP));
                    }

                    // Revenue growth — needs two years of data
                    if (twoLatest.size() == 2) {
                        BigDecimal prevRevenue = twoLatest.get(1).getRevenue();
                        BigDecimal currRevenue = twoLatest.get(0).getRevenue();
                        if (prevRevenue != null && prevRevenue.compareTo(BigDecimal.ZERO) != 0 && currRevenue != null) {
                            metric.setRevenueGrowth(currRevenue.subtract(prevRevenue).divide(prevRevenue.abs(), 4, RoundingMode.HALF_UP));
                        }
                    }
                }

                // PER and PBR from stock market data (market cap / earnings and book value)
                if (stock.getMarketCap() != null) {
                    if (latest != null && latest.getNetIncome() != null && latest.getNetIncome().compareTo(BigDecimal.ZERO) > 0) {
                        metric.setPer(stock.getMarketCap().divide(latest.getNetIncome(), 4, RoundingMode.HALF_UP));
                    }
                    if (latest != null && latest.getTotalEquity() != null && latest.getTotalEquity().compareTo(BigDecimal.ZERO) > 0) {
                        metric.setPbr(stock.getMarketCap().divide(latest.getTotalEquity(), 4, RoundingMode.HALF_UP));
                    }
                }

                stockMetricRepository.save(metric);
            } catch (Exception e) {
                log.error("Failed to calculate metrics for {}: {}", stock.getTicker(), e.getMessage());
            }
        }
        log.info("Stock metrics calculated for {} stocks", stocks.size());
    }

    @Transactional
    public void calculateSectorMetrics() {
        List<Sector> sectors = sectorRepository.findAll();
        LocalDate today = LocalDate.now();

        int deleted = sectorMetricRepository.deleteByDate(today);
        if (deleted > 0) {
            log.info("Cleared {} existing sector_metrics rows for {}", deleted, today);
        }

        for (Sector sector : sectors) {
            try {
                List<StockMetric> metrics = stockMetricRepository.findByFilters(
                        buildSectorFilter(sector.getId()), 0, Integer.MAX_VALUE, null);

                if (metrics.isEmpty()) continue;

                SectorMetric sm = new SectorMetric();
                sm.setSector(sector);
                sm.setDate(today);
                sm.setAvgPer(average(metrics.stream().map(StockMetric::getPer).collect(Collectors.toList())));
                sm.setAvgPbr(average(metrics.stream().map(StockMetric::getPbr).collect(Collectors.toList())));
                sm.setAvgDividendYield(average(metrics.stream().map(StockMetric::getDividendYield).collect(Collectors.toList())));

                BigDecimal totalCap = metrics.stream()
                        .map(m -> m.getStock().getMarketCap())
                        .filter(v -> v != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                sm.setTotalMarketCap(totalCap);

                sectorMetricRepository.save(sm);
            } catch (Exception e) {
                log.error("Failed to calculate sector metrics for {}: {}", sector.getName(), e.getMessage());
            }
        }
        log.info("Sector metrics calculated for {} sectors", sectors.size());
    }

    @Transactional
    public void calculateIndustryMetrics() {
        List<Sector> sectors = sectorRepository.findAll();
        LocalDate today = LocalDate.now();

        int deleted = industryMetricRepository.deleteByDate(today);
        if (deleted > 0) {
            log.info("Cleared {} existing industry_metrics rows for {}", deleted, today);
        }

        for (Sector sector : sectors) {
            List<Industry> industries = industryRepository.findBySectorId(sector.getId());
            for (Industry industry : industries) {
                try {
                    List<StockMetric> metrics = stockMetricRepository.findByFilters(
                            buildIndustryFilter(industry), 0, Integer.MAX_VALUE, null);

                    if (metrics.isEmpty()) continue;

                    IndustryMetric im = new IndustryMetric();
                    im.setIndustry(industry);
                    im.setDate(today);
                    im.setAvgPer(average(metrics.stream().map(StockMetric::getPer).collect(Collectors.toList())));
                    im.setAvgPbr(average(metrics.stream().map(StockMetric::getPbr).collect(Collectors.toList())));
                    im.setStockCount(metrics.size());

                    BigDecimal totalCap = metrics.stream()
                            .map(m -> m.getStock().getMarketCap())
                            .filter(v -> v != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    im.setTotalMarketCap(totalCap);

                    industryMetricRepository.save(im);
                } catch (Exception e) {
                    log.error("Failed to calculate industry metrics for {}: {}", industry.getName(), e.getMessage());
                }
            }
        }
        log.info("Industry metrics calculated");
    }

    private com.stockanalyzer.dto.ScreeningFilter buildSectorFilter(Long sectorId) {
        com.stockanalyzer.dto.ScreeningFilter f = new com.stockanalyzer.dto.ScreeningFilter();
        f.setSectorId(sectorId);
        return f;
    }

    private com.stockanalyzer.dto.ScreeningFilter buildIndustryFilter(Industry industry) {
        com.stockanalyzer.dto.ScreeningFilter f = new com.stockanalyzer.dto.ScreeningFilter();
        f.setIndustryId(industry.getId());
        return f;
    }

    private BigDecimal average(List<BigDecimal> values) {
        List<BigDecimal> nonNull = values.stream().filter(v -> v != null).collect(Collectors.toList());
        if (nonNull.isEmpty()) return null;
        BigDecimal sum = nonNull.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(nonNull.size()), 4, RoundingMode.HALF_UP);
    }
}
