package com.stockanalyzer.controller;

import com.stockanalyzer.common.ApiResponse;
import com.stockanalyzer.dto.IndexQuote;
import com.stockanalyzer.dto.MoverStock;
import com.stockanalyzer.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/indices")
    public ApiResponse<List<IndexQuote>> getIndices() {
        return ApiResponse.ok(dashboardService.getIndices());
    }

    @GetMapping("/movers")
    public ApiResponse<Map<String, List<MoverStock>>> getMovers() {
        return ApiResponse.ok(dashboardService.getMovers());
    }

    @GetMapping("/extremes")
    public ApiResponse<Map<String, List<MoverStock>>> getExtremes() {
        return ApiResponse.ok(dashboardService.getExtremes());
    }

    @GetMapping("/volume-spikes")
    public ApiResponse<List<MoverStock>> getVolumeSpikes() {
        return ApiResponse.ok(dashboardService.getVolumeSpikes());
    }
}
