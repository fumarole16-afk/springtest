package com.stockanalyzer.service;

import com.stockanalyzer.dto.CompareData;
import com.stockanalyzer.dto.FinancialData;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CompareServiceTest {

    @Mock private StockService stockService;
    @Mock private FinancialService financialService;
    @InjectMocks private CompareService compareService;

    @Before
    public void setUp() { MockitoAnnotations.initMocks(this); }

    @Test
    public void compare_callsStockAndFinancialServiceForEachTicker() {
        StockDetail aaplDetail = new StockDetail();
        aaplDetail.setTicker("AAPL");
        aaplDetail.setCurrentPrice(new BigDecimal("175.00"));

        StockDetail msftDetail = new StockDetail();
        msftDetail.setTicker("MSFT");
        msftDetail.setCurrentPrice(new BigDecimal("420.00"));

        PriceData priceData = new PriceData();
        priceData.setClose(new BigDecimal("175.00"));

        FinancialData financialData = new FinancialData();
        financialData.setRevenue(new BigDecimal("400000000000"));

        when(stockService.getDetail("AAPL")).thenReturn(aaplDetail);
        when(stockService.getDetail("MSFT")).thenReturn(msftDetail);
        when(stockService.getPrices(eq("AAPL"), anyString())).thenReturn(Collections.singletonList(priceData));
        when(stockService.getPrices(eq("MSFT"), anyString())).thenReturn(Collections.emptyList());
        when(financialService.getFinancials(eq("AAPL"), anyString())).thenReturn(Collections.singletonList(financialData));
        when(financialService.getFinancials(eq("MSFT"), anyString())).thenReturn(Collections.emptyList());

        CompareData result = compareService.compare(Arrays.asList("AAPL", "MSFT"), "1m");

        assertEquals(2, result.getStocks().size());
        assertEquals("AAPL", result.getStocks().get(0).getTicker());
        assertEquals("MSFT", result.getStocks().get(1).getTicker());

        assertTrue(result.getPrices().containsKey("AAPL"));
        assertTrue(result.getPrices().containsKey("MSFT"));
        assertEquals(1, result.getPrices().get("AAPL").size());

        assertTrue(result.getFinancials().containsKey("AAPL"));
        assertTrue(result.getFinancials().containsKey("MSFT"));
        assertEquals(1, result.getFinancials().get("AAPL").size());

        verify(stockService, times(1)).getDetail("AAPL");
        verify(stockService, times(1)).getDetail("MSFT");
        verify(financialService, times(1)).getFinancials("AAPL", "annual");
        verify(financialService, times(1)).getFinancials("MSFT", "annual");
    }

    @Test
    public void compare_singleTicker_returnsCorrectStructure() {
        StockDetail detail = new StockDetail();
        detail.setTicker("GOOGL");

        when(stockService.getDetail("GOOGL")).thenReturn(detail);
        when(stockService.getPrices(eq("GOOGL"), anyString())).thenReturn(Collections.emptyList());
        when(financialService.getFinancials(eq("GOOGL"), anyString())).thenReturn(Collections.emptyList());

        CompareData result = compareService.compare(Arrays.asList("GOOGL"), "3m");

        assertEquals(1, result.getStocks().size());
        verify(stockService).getPrices("GOOGL", "3m");
    }
}
