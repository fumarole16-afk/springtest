package com.stockanalyzer.service;

import com.stockanalyzer.client.YahooFinanceClient;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import com.stockanalyzer.dto.StockSearchResult;
import com.stockanalyzer.entity.DailyPrice;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.repository.DailyPriceRepository;
import com.stockanalyzer.repository.StockRepository;
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

public class StockServiceTest {
    @Mock private StockRepository stockRepository;
    @Mock private DailyPriceRepository dailyPriceRepository;
    @Mock private YahooFinanceClient yahooClient;
    @InjectMocks private StockService stockService;

    @Before
    public void setUp() { MockitoAnnotations.initMocks(this); }

    @Test
    public void search_returnsStockSearchResults() {
        Stock stock = new Stock();
        stock.setTicker("AAPL");
        stock.setCompanyName("Apple Inc.");
        when(stockRepository.search("AAPL", 10)).thenReturn(Arrays.asList(stock));
        List<StockSearchResult> results = stockService.search("AAPL");
        assertEquals(1, results.size());
        assertEquals("AAPL", results.get(0).getTicker());
        assertEquals("Apple Inc.", results.get(0).getCompanyName());
    }

    @Test
    public void getDetail_whenStockExistsInDB_enrichesWithYahooData() {
        Stock stock = new Stock();
        stock.setTicker("AAPL");
        stock.setCompanyName("Apple Inc.");
        when(stockRepository.findByTicker("AAPL")).thenReturn(stock);
        StockDetail yahooDetail = new StockDetail();
        yahooDetail.setCurrentPrice(new BigDecimal("175.00"));
        yahooDetail.setTrailingPE(new BigDecimal("28.5"));
        when(yahooClient.fetchQuote("AAPL")).thenReturn(yahooDetail);
        StockDetail result = stockService.getDetail("AAPL");
        assertEquals("AAPL", result.getTicker());
        assertEquals(new BigDecimal("175.00"), result.getCurrentPrice());
    }

    @Test
    public void getPrices_returnsFromDB() {
        DailyPrice dp = new DailyPrice();
        dp.setDate(LocalDate.of(2026, 3, 20));
        dp.setOpen(new BigDecimal("170.00"));
        dp.setClose(new BigDecimal("173.00"));
        dp.setHigh(new BigDecimal("175.00"));
        dp.setLow(new BigDecimal("168.00"));
        dp.setVolume(50000000L);
        dp.setAdjustedClose(new BigDecimal("173.00"));
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setTicker("AAPL");
        when(stockRepository.findByTicker("AAPL")).thenReturn(stock);
        when(dailyPriceRepository.findByStockAndDateRange(eq(1L), any(), any()))
                .thenReturn(Arrays.asList(dp));
        List<PriceData> prices = stockService.getPrices("AAPL", "1m");
        assertEquals(1, prices.size());
        assertEquals(new BigDecimal("173.00"), prices.get(0).getClose());
    }
}
