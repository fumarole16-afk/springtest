package com.stockanalyzer.controller;

import com.stockanalyzer.common.ApiResponse;
import com.stockanalyzer.dto.CompareData;
import com.stockanalyzer.service.CompareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compare")
public class CompareController {

    private final CompareService compareService;

    @Autowired
    public CompareController(CompareService compareService) {
        this.compareService = compareService;
    }

    @GetMapping
    public ApiResponse<CompareData> compare(
            @RequestParam("tickers") String tickersParam,
            @RequestParam(value = "period", defaultValue = "1m") String period) {
        List<String> tickers = Arrays.stream(tickersParam.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .limit(5)
                .collect(Collectors.toList());

        if (tickers.isEmpty()) {
            return ApiResponse.error("At least one ticker is required");
        }

        return ApiResponse.ok(compareService.compare(tickers, period));
    }
}
