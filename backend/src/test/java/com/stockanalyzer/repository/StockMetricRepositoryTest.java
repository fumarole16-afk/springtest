package com.stockanalyzer.repository;

import com.stockanalyzer.dto.ScreeningFilter;
import com.stockanalyzer.entity.Industry;
import com.stockanalyzer.entity.Sector;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.entity.StockMetric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class StockMetricRepositoryTest {

    @Autowired private StockMetricRepository stockMetricRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private IndustryRepository industryRepository;

    private Sector tech;

    @Before
    public void setUp() {
        tech = sectorRepository.save(new Sector("Technology", "Tech"));
        Industry sw = industryRepository.save(new Industry("Software", "SW", tech));

        Stock apple = new Stock();
        apple.setTicker("AAPL");
        apple.setCompanyName("Apple Inc.");
        apple.setExchange("NASDAQ");
        apple.setMarketCap(new BigDecimal("3000000000000"));
        apple.setIndustry(sw);
        apple = stockRepository.save(apple);

        StockMetric m1 = new StockMetric();
        m1.setStock(apple);
        m1.setDate(LocalDate.of(2026, 3, 28));
        m1.setPer(new BigDecimal("28.5"));
        m1.setPbr(new BigDecimal("45.2"));
        m1.setDividendYield(new BigDecimal("0.55"));
        m1.setRoe(new BigDecimal("0.285"));
        m1.setDebtRatio(new BigDecimal("4.67"));
        m1.setRevenueGrowth(new BigDecimal("0.08"));
        m1.setOperatingMargin(new BigDecimal("0.29"));
        m1.setWeek52High(new BigDecimal("199.62"));
        m1.setWeek52Low(new BigDecimal("140.00"));
        m1.setAvgVolume30d(55000000L);
        stockMetricRepository.save(m1);

        Stock msft = new Stock();
        msft.setTicker("MSFT");
        msft.setCompanyName("Microsoft Corporation");
        msft.setExchange("NASDAQ");
        msft.setMarketCap(new BigDecimal("2800000000000"));
        msft.setIndustry(sw);
        msft = stockRepository.save(msft);

        StockMetric m2 = new StockMetric();
        m2.setStock(msft);
        m2.setDate(LocalDate.of(2026, 3, 28));
        m2.setPer(new BigDecimal("35.0"));
        m2.setPbr(new BigDecimal("12.0"));
        m2.setDividendYield(new BigDecimal("0.72"));
        m2.setRoe(new BigDecimal("0.38"));
        m2.setDebtRatio(new BigDecimal("1.50"));
        m2.setRevenueGrowth(new BigDecimal("0.15"));
        m2.setOperatingMargin(new BigDecimal("0.42"));
        m2.setWeek52High(new BigDecimal("420.00"));
        m2.setWeek52Low(new BigDecimal("310.00"));
        m2.setAvgVolume30d(25000000L);
        stockMetricRepository.save(m2);
    }

    @Test
    public void findByFilters_noFilters_returnsAll() {
        ScreeningFilter filter = new ScreeningFilter();
        List<StockMetric> results = stockMetricRepository.findByFilters(filter, 0, 20, "marketCap,desc");
        assertEquals(2, results.size());
    }

    @Test
    public void findByFilters_maxPer30_returnsOnlyApple() {
        ScreeningFilter filter = new ScreeningFilter();
        filter.setMaxPer(new BigDecimal("30"));
        List<StockMetric> results = stockMetricRepository.findByFilters(filter, 0, 20, "marketCap,desc");
        assertEquals(1, results.size());
        assertEquals("AAPL", results.get(0).getStock().getTicker());
    }

    @Test
    public void findByFilters_minRoe035_returnsOnlyMsft() {
        ScreeningFilter filter = new ScreeningFilter();
        filter.setMinRoe(new BigDecimal("0.35"));
        List<StockMetric> results = stockMetricRepository.findByFilters(filter, 0, 20, "marketCap,desc");
        assertEquals(1, results.size());
        assertEquals("MSFT", results.get(0).getStock().getTicker());
    }

    @Test
    public void countByFilters_returnsCorrectCount() {
        ScreeningFilter filter = new ScreeningFilter();
        long count = stockMetricRepository.countByFilters(filter);
        assertEquals(2, count);
    }
}
