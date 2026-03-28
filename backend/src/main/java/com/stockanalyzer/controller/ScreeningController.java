package com.stockanalyzer.controller;

import com.stockanalyzer.common.ApiResponse;
import com.stockanalyzer.dto.ScreeningFilter;
import com.stockanalyzer.dto.ScreeningResult;
import com.stockanalyzer.service.ScreeningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/screening")
public class ScreeningController {

    private final ScreeningService screeningService;

    @Autowired
    public ScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    @GetMapping
    public ApiResponse<List<ScreeningResult>> screen(
            @RequestParam(required = false) Long sectorId,
            @RequestParam(required = false) BigDecimal minPer,
            @RequestParam(required = false) BigDecimal maxPer,
            @RequestParam(required = false) BigDecimal minPbr,
            @RequestParam(required = false) BigDecimal maxPbr,
            @RequestParam(required = false) BigDecimal minCap,
            @RequestParam(required = false) BigDecimal maxCap,
            @RequestParam(required = false) BigDecimal minDividendYield,
            @RequestParam(required = false) BigDecimal minRoe,
            @RequestParam(required = false) BigDecimal maxDebtRatio,
            @RequestParam(required = false) BigDecimal minRevenueGrowth,
            @RequestParam(required = false) BigDecimal minOperatingMargin,
            @RequestParam(required = false) String exchange,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {

        ScreeningFilter filter = new ScreeningFilter();
        filter.setSectorId(sectorId);
        filter.setMinPer(minPer);
        filter.setMaxPer(maxPer);
        filter.setMinPbr(minPbr);
        filter.setMaxPbr(maxPbr);
        filter.setMinCap(minCap);
        filter.setMaxCap(maxCap);
        filter.setMinDividendYield(minDividendYield);
        filter.setMinRoe(minRoe);
        filter.setMaxDebtRatio(maxDebtRatio);
        filter.setMinRevenueGrowth(minRevenueGrowth);
        filter.setMinOperatingMargin(minOperatingMargin);
        filter.setExchange(exchange);

        List<ScreeningResult> results = screeningService.screen(filter, page, size, sort);
        long totalCount = screeningService.count(filter);
        int totalPages = (int) Math.ceil((double) totalCount / size);

        Map<String, Object> meta = new HashMap<>();
        meta.put("page", page);
        meta.put("size", size);
        meta.put("totalCount", totalCount);
        meta.put("totalPages", totalPages);

        return ApiResponse.ok(results, meta);
    }
}
