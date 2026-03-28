package com.stockanalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockanalyzer.dto.ScreeningFilter;
import com.stockanalyzer.dto.ScreeningResult;
import com.stockanalyzer.service.ScreeningService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ScreeningControllerTest {
    private MockMvc mockMvc;
    @Mock private ScreeningService screeningService;
    @InjectMocks private ScreeningController screeningController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(screeningController).build();
    }

    @Test
    public void screen_noFilters_returnsResults() throws Exception {
        ScreeningResult r = new ScreeningResult();
        r.setTicker("AAPL");
        r.setCompanyName("Apple Inc.");
        r.setPer(new BigDecimal("25.00"));

        when(screeningService.screen(any(ScreeningFilter.class), anyInt(), anyInt(), isNull()))
                .thenReturn(Arrays.asList(r));
        when(screeningService.count(any(ScreeningFilter.class))).thenReturn(1L);

        mockMvc.perform(get("/api/screening").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$.meta.totalCount").value(1))
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(20));
    }

    @Test
    public void screen_withFilters_passesFilterToService() throws Exception {
        when(screeningService.screen(any(ScreeningFilter.class), anyInt(), anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
        when(screeningService.count(any(ScreeningFilter.class))).thenReturn(0L);

        mockMvc.perform(get("/api/screening")
                        .param("minPer", "10")
                        .param("maxPer", "30")
                        .param("minRoe", "0.15")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "per,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(10))
                .andExpect(jsonPath("$.meta.totalPages").value(0));
    }
}
