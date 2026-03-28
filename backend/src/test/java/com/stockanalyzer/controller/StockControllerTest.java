package com.stockanalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import com.stockanalyzer.dto.StockSearchResult;
import com.stockanalyzer.service.FinancialService;
import com.stockanalyzer.service.StockService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class StockControllerTest {
    private MockMvc mockMvc;
    @Mock private StockService stockService;
    @Mock private FinancialService financialService;
    @InjectMocks private StockController stockController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(stockController)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    public void searchStocks_returnsResults() throws Exception {
        when(stockService.search("AAPL")).thenReturn(
                Arrays.asList(new StockSearchResult("AAPL", "Apple Inc.", "NASDAQ")));
        mockMvc.perform(get("/api/stocks").param("q", "AAPL").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].ticker").value("AAPL"));
    }

    @Test
    public void getStockDetail_returnsDetail() throws Exception {
        StockDetail detail = new StockDetail();
        detail.setTicker("AAPL");
        detail.setCompanyName("Apple Inc.");
        detail.setCurrentPrice(new BigDecimal("175.00"));
        when(stockService.getDetail("AAPL")).thenReturn(detail);
        mockMvc.perform(get("/api/stocks/AAPL").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.currentPrice").value(175.00));
    }

    @Test
    public void getStockPrices_returnsPriceList() throws Exception {
        PriceData pd = new PriceData();
        pd.setDate(LocalDate.of(2026, 3, 20));
        pd.setClose(new BigDecimal("173.00"));
        pd.setOpen(new BigDecimal("170.00"));
        pd.setHigh(new BigDecimal("175.00"));
        pd.setLow(new BigDecimal("168.00"));
        pd.setVolume(50000000L);
        when(stockService.getPrices("AAPL", "1m")).thenReturn(Arrays.asList(pd));
        mockMvc.perform(get("/api/stocks/AAPL/prices").param("period", "1m").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].close").value(173.00));
    }
}
