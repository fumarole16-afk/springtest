package com.stockanalyzer.service;

import com.stockanalyzer.dto.ScreeningFilter;
import com.stockanalyzer.dto.ScreeningResult;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.entity.StockMetric;
import com.stockanalyzer.repository.StockMetricRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ScreeningServiceTest {

    @Mock private StockMetricRepository stockMetricRepository;
    @InjectMocks private ScreeningService screeningService;

    private StockMetric appleMetric;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Stock apple = new Stock();
        apple.setTicker("AAPL");
        apple.setCompanyName("Apple Inc.");
        apple.setExchange("NASDAQ");
        apple.setMarketCap(new BigDecimal("3000000000000"));

        appleMetric = new StockMetric();
        appleMetric.setStock(apple);
        appleMetric.setDate(LocalDate.of(2026, 3, 28));
        appleMetric.setPer(new BigDecimal("28.5"));
        appleMetric.setPbr(new BigDecimal("45.2"));
        appleMetric.setRoe(new BigDecimal("0.285"));
        appleMetric.setDebtRatio(new BigDecimal("4.67"));
        appleMetric.setDividendYield(new BigDecimal("0.55"));
        appleMetric.setRevenueGrowth(new BigDecimal("0.08"));
        appleMetric.setOperatingMargin(new BigDecimal("0.29"));
    }

    @Test
    public void screen_delegatesToRepository_andConvertsToDto() {
        ScreeningFilter filter = new ScreeningFilter();
        when(stockMetricRepository.findByFilters(filter, 0, 20, "marketCap,desc"))
                .thenReturn(Arrays.asList(appleMetric));

        List<ScreeningResult> results = screeningService.screen(filter, 0, 20, "marketCap,desc");

        assertEquals(1, results.size());
        ScreeningResult r = results.get(0);
        assertEquals("AAPL", r.getTicker());
        assertEquals("Apple Inc.", r.getCompanyName());
        assertEquals("NASDAQ", r.getExchange());
        assertEquals(new BigDecimal("3000000000000"), r.getMarketCap());
        assertEquals(new BigDecimal("28.5"), r.getPer());
        assertEquals(new BigDecimal("0.285"), r.getRoe());

        verify(stockMetricRepository).findByFilters(filter, 0, 20, "marketCap,desc");
    }

    @Test
    public void count_delegatesToRepository() {
        ScreeningFilter filter = new ScreeningFilter();
        when(stockMetricRepository.countByFilters(filter)).thenReturn(42L);

        long count = screeningService.count(filter);

        assertEquals(42L, count);
        verify(stockMetricRepository).countByFilters(filter);
    }
}
