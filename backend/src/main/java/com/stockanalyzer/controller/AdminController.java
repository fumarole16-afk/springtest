package com.stockanalyzer.controller;

import com.stockanalyzer.common.ApiResponse;
import com.stockanalyzer.scheduler.FinancialCollector;
import com.stockanalyzer.scheduler.MetricsCalculator;
import com.stockanalyzer.scheduler.PriceCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final PriceCollector priceCollector;
    private final FinancialCollector financialCollector;
    private final MetricsCalculator metricsCalculator;

    @Autowired
    public AdminController(PriceCollector priceCollector,
                           FinancialCollector financialCollector,
                           MetricsCalculator metricsCalculator) {
        this.priceCollector = priceCollector;
        this.financialCollector = financialCollector;
        this.metricsCalculator = metricsCalculator;
    }

    @PostMapping("/collect")
    public ApiResponse<String> collectAll() {
        new Thread(() -> {
            try {
                log.info("Manual collection started");
                priceCollector.collectDailyPrices();
                financialCollector.collectFinancials();
                metricsCalculator.calculateStockMetrics();
                metricsCalculator.calculateSectorMetrics();
                metricsCalculator.calculateIndustryMetrics();
                log.info("Manual collection completed");
            } catch (Exception e) {
                log.error("Manual collection failed: {}", e.getMessage(), e);
            }
        }).start();
        return ApiResponse.ok("Collection started in background");
    }
}
