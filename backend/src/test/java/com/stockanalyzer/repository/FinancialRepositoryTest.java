package com.stockanalyzer.repository;

import com.stockanalyzer.entity.Financial;
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
public class FinancialRepositoryTest {

    @Autowired private FinancialRepository financialRepository;
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

        Financial f1 = new Financial();
        f1.setStock(apple);
        f1.setPeriod("2025");
        f1.setType("annual");
        f1.setRevenue(new BigDecimal("394328000000"));
        f1.setOperatingIncome(new BigDecimal("114301000000"));
        f1.setNetIncome(new BigDecimal("96995000000"));
        f1.setTotalAssets(new BigDecimal("352583000000"));
        f1.setTotalLiabilities(new BigDecimal("290437000000"));
        f1.setTotalEquity(new BigDecimal("62146000000"));
        f1.setOperatingCashFlow(new BigDecimal("110543000000"));
        financialRepository.save(f1);

        Financial f2 = new Financial();
        f2.setStock(apple);
        f2.setPeriod("2024");
        f2.setType("annual");
        f2.setRevenue(new BigDecimal("365000000000"));
        f2.setOperatingIncome(new BigDecimal("105000000000"));
        f2.setNetIncome(new BigDecimal("90000000000"));
        f2.setTotalAssets(new BigDecimal("340000000000"));
        f2.setTotalLiabilities(new BigDecimal("280000000000"));
        f2.setTotalEquity(new BigDecimal("60000000000"));
        f2.setOperatingCashFlow(new BigDecimal("100000000000"));
        financialRepository.save(f2);
    }

    @Test
    public void findByStockAndType_returnsAnnualFinancials() {
        List<Financial> results = financialRepository.findByStockAndType(apple.getId(), "annual");
        assertEquals(2, results.size());
        assertEquals("2025", results.get(0).getPeriod());
    }

    @Test
    public void findLatestByStock_returnsNewestPeriod() {
        Financial latest = financialRepository.findLatestByStock(apple.getId(), "annual");
        assertNotNull(latest);
        assertEquals("2025", latest.getPeriod());
    }
}
