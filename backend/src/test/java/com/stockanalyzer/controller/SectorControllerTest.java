package com.stockanalyzer.controller;

import com.stockanalyzer.dto.IndustryDetail;
import com.stockanalyzer.dto.SectorDetail;
import com.stockanalyzer.dto.SectorOverview;
import com.stockanalyzer.service.SectorService;
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
import java.util.HashMap;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class SectorControllerTest {
    private MockMvc mockMvc;
    @Mock private SectorService sectorService;
    @InjectMocks private SectorController sectorController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(sectorController).build();
    }

    @Test
    public void getAllSectors_returnsList() throws Exception {
        SectorOverview overview = new SectorOverview();
        overview.setSectorId(1L);
        overview.setSectorName("Technology");
        overview.setAvgPer(new BigDecimal("25.00"));

        when(sectorService.getAllSectors()).thenReturn(Arrays.asList(overview));

        mockMvc.perform(get("/api/sectors").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sectorName").value("Technology"));
    }

    @Test
    public void getSectorDetail_found_returnsDetail() throws Exception {
        SectorDetail detail = new SectorDetail();
        detail.setSectorId(1L);
        detail.setSectorName("Technology");
        detail.setIndustries(Collections.emptyList());

        when(sectorService.getSectorDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/sectors/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sectorName").value("Technology"));
    }

    @Test
    public void getSectorDetail_notFound_returnsError() throws Exception {
        when(sectorService.getSectorDetail(99L)).thenReturn(null);

        mockMvc.perform(get("/api/sectors/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void getSectorRankings_returnsRankings() throws Exception {
        SectorOverview overview = new SectorOverview();
        overview.setSectorId(1L);
        overview.setSectorName("Technology");

        when(sectorService.getSectorRankings(1L, "gainers")).thenReturn(Arrays.asList(overview));

        mockMvc.perform(get("/api/sectors/1/rankings").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sectorId").value(1));
    }

    @Test
    public void getIndustryDetail_found_returnsDetail() throws Exception {
        IndustryDetail detail = new IndustryDetail();
        detail.setIndustryId(10L);
        detail.setIndustryName("Software");
        detail.setPerformances(new HashMap<>());

        when(sectorService.getIndustryDetail(10L)).thenReturn(detail);

        mockMvc.perform(get("/api/industries/10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.industryName").value("Software"));
    }
}
