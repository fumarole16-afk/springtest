package com.stockanalyzer.repository;

import com.stockanalyzer.entity.DailyPrice;
import com.stockanalyzer.entity.Industry;
import com.stockanalyzer.entity.Sector;
import com.stockanalyzer.entity.Stock;
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
public class DailyPriceRepositoryTest {
    @Autowired private DailyPriceRepository dailyPriceRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private IndustryRepository industryRepository;

    private Stock apple;

    @Before
    public void setUp() {
        Sector tech = sectorRepository.save(new Sector("Technology", "Tech"));
        Industry sw = industryRepository.save(new Industry("Software", "SW", tech));
        apple = new Stock();
        apple.setTicker("AAPL");
        apple.setCompanyName("Apple Inc.");
        apple.setExchange("NASDAQ");
        apple.setMarketCap(new BigDecimal("3000000000000"));
        apple.setIndustry(sw);
        apple = stockRepository.save(apple);

        for (int i = 0; i < 5; i++) {
            DailyPrice dp = new DailyPrice();
            dp.setStock(apple);
            dp.setDate(LocalDate.of(2026, 3, 20 + i));
            dp.setOpen(new BigDecimal("170.00").add(new BigDecimal(i)));
            dp.setHigh(new BigDecimal("175.00").add(new BigDecimal(i)));
            dp.setLow(new BigDecimal("168.00").add(new BigDecimal(i)));
            dp.setClose(new BigDecimal("173.00").add(new BigDecimal(i)));
            dp.setAdjustedClose(new BigDecimal("173.00").add(new BigDecimal(i)));
            dp.setVolume(50000000L + i * 1000000L);
            dailyPriceRepository.save(dp);
        }
    }

    @Test
    public void findByStockAndDateRange_returnsCorrectPrices() {
        List<DailyPrice> prices = dailyPriceRepository.findByStockAndDateRange(
                apple.getId(), LocalDate.of(2026, 3, 21), LocalDate.of(2026, 3, 23));
        assertEquals(3, prices.size());
    }

    @Test
    public void findLatestByStock_returnsNewestDate() {
        DailyPrice latest = dailyPriceRepository.findLatestByStock(apple.getId());
        assertNotNull(latest);
        assertEquals(LocalDate.of(2026, 3, 24), latest.getDate());
    }
}
