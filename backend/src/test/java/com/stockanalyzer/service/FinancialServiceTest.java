package com.stockanalyzer.service;

import com.stockanalyzer.dto.FinancialData;
import com.stockanalyzer.dto.FinancialRatios;
import com.stockanalyzer.entity.Financial;
import com.stockanalyzer.entity.Industry;
import com.stockanalyzer.entity.Sector;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.repository.FinancialRepository;
import com.stockanalyzer.repository.StockMetricRepository;
import com.stockanalyzer.repository.StockRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FinancialServiceTest {

    @Mock private FinancialRepository financialRepository;
    @Mock private StockRepository stockRepository;
    @Mock private StockMetricRepository stockMetricRepository;
    @InjectMocks private FinancialService financialService;

    private Stock apple;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Sector tech = new Sector("Technology", "Tech");
        tech.setId(1L);
        Industry sw = new Industry("Software", "SW", tech);
        sw.setId(1L);
        apple = new Stock();
        apple.setId(1L);
        apple.setTicker("AAPL");
        apple.setCompanyName("Apple Inc.");
        apple.setIndustry(sw);
    }

    @Test
    public void getFinancials_returnsConvertedDTOs() {
        Financial f = new Financial();
        f.setStock(apple);
        f.setPeriod("2025");
        f.setType("annual");
        f.setRevenue(new BigDecimal("394328000000"));
        f.setNetIncome(new BigDecimal("96995000000"));
        f.setOperatingIncome(new BigDecimal("114301000000"));
        f.setTotalAssets(new BigDecimal("352583000000"));
        f.setTotalLiabilities(new BigDecimal("290437000000"));
        f.setTotalEquity(new BigDecimal("62146000000"));
        f.setOperatingCashFlow(new BigDecimal("110543000000"));

        when(stockRepository.findByTicker("AAPL")).thenReturn(apple);
        when(financialRepository.findByStockAndType(1L, "annual")).thenReturn(Arrays.asList(f));

        List<FinancialData> results = financialService.getFinancials("AAPL", "annual");
        assertEquals(1, results.size());
        assertEquals("2025", results.get(0).getPeriod());
        assertEquals(new BigDecimal("394328000000"), results.get(0).getRevenue());
    }

    @Test
    public void getRatios_calculatesCorrectly() {
        Financial f = new Financial();
        f.setStock(apple);
        f.setPeriod("2025");
        f.setType("annual");
        f.setRevenue(new BigDecimal("394328000000"));
        f.setOperatingIncome(new BigDecimal("114301000000"));
        f.setNetIncome(new BigDecimal("96995000000"));
        f.setTotalAssets(new BigDecimal("352583000000"));
        f.setTotalLiabilities(new BigDecimal("290437000000"));
        f.setTotalEquity(new BigDecimal("62146000000"));
        f.setOperatingCashFlow(new BigDecimal("110543000000"));

        when(stockRepository.findByTicker("AAPL")).thenReturn(apple);
        when(financialRepository.findLatestByStock(1L, "annual")).thenReturn(f);

        FinancialRatios ratios = financialService.getRatios("AAPL");
        assertNotNull(ratios.getStock());
        assertTrue(ratios.getStock().get("roe").compareTo(BigDecimal.ZERO) > 0);
        assertTrue(ratios.getStock().get("operatingMargin").compareTo(BigDecimal.ZERO) > 0);
    }
}
