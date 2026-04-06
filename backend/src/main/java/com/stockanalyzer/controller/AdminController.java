package com.stockanalyzer.controller;

import com.stockanalyzer.common.ApiResponse;
import com.stockanalyzer.scheduler.FinancialCollector;
import com.stockanalyzer.scheduler.MetricsCalculator;
import com.stockanalyzer.scheduler.PriceCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final PriceCollector priceCollector;
    private final FinancialCollector financialCollector;
    private final MetricsCalculator metricsCalculator;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    public AdminController(PriceCollector priceCollector,
                           FinancialCollector financialCollector,
                           MetricsCalculator metricsCalculator,
                           DataSource dataSource) {
        this.priceCollector = priceCollector;
        this.financialCollector = financialCollector;
        this.metricsCalculator = metricsCalculator;
        this.dataSource = dataSource;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            Connection conn = dataSource.getConnection();
            info.put("jdbcUrl", conn.getMetaData().getURL());
            info.put("dbUser", conn.getMetaData().getUserName());
            conn.close();
        } catch (Exception e) {
            info.put("dbError", e.getMessage());
        }
        info.put("envDbUrl", System.getenv("DB_URL"));
        try {
            long stockCount = (long) em.createQuery("SELECT COUNT(s) FROM Stock s").getSingleResult();
            info.put("stockCount", stockCount);
            long sectorCount = (long) em.createQuery("SELECT COUNT(s) FROM Sector s").getSingleResult();
            info.put("sectorCount", sectorCount);
        } catch (Exception e) {
            info.put("jpaError", e.getMessage());
        }
        try {
            java.sql.Connection conn = dataSource.getConnection();
            java.sql.ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM stocks");
            rs.next();
            info.put("rawStockCount", rs.getInt(1));
            rs.close();
            conn.close();
        } catch (Exception e) {
            info.put("rawSqlError", e.getMessage());
        }
        return ApiResponse.ok(info);
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
