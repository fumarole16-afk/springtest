package com.stockanalyzer.repository;

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
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class StockRepositoryTest {
    @Autowired private StockRepository stockRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private IndustryRepository industryRepository;

    private Stock apple;

    @Before
    public void setUp() {
        Sector tech = sectorRepository.save(new Sector("Technology", "Tech sector"));
        Industry software = industryRepository.save(new Industry("Software", "Software", tech));
        apple = new Stock();
        apple.setTicker("AAPL");
        apple.setCompanyName("Apple Inc.");
        apple.setExchange("NASDAQ");
        apple.setMarketCap(new BigDecimal("3000000000000"));
        apple.setIndustry(software);
        apple = stockRepository.save(apple);

        Stock msft = new Stock();
        msft.setTicker("MSFT");
        msft.setCompanyName("Microsoft Corporation");
        msft.setExchange("NASDAQ");
        msft.setMarketCap(new BigDecimal("2800000000000"));
        msft.setIndustry(software);
        stockRepository.save(msft);
    }

    @Test
    public void searchByTicker_returnsMatch() {
        List<Stock> results = stockRepository.search("AAPL", 10);
        assertEquals(1, results.size());
        assertEquals("AAPL", results.get(0).getTicker());
    }
    @Test
    public void searchByCompanyName_returnsMatch() {
        List<Stock> results = stockRepository.search("Apple", 10);
        assertEquals(1, results.size());
        assertEquals("Apple Inc.", results.get(0).getCompanyName());
    }
    @Test
    public void searchPartial_returnsBothMatches() {
        List<Stock> results = stockRepository.search("M", 10);
        assertEquals(1, results.size());
    }
    @Test
    public void findByTicker_returnsStock() {
        Stock found = stockRepository.findByTicker("AAPL");
        assertNotNull(found);
        assertEquals("Apple Inc.", found.getCompanyName());
    }
    @Test
    public void findByTicker_notFound_returnsNull() {
        Stock found = stockRepository.findByTicker("ZZZZ");
        assertNull(found);
    }
}
