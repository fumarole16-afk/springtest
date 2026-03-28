package com.stockanalyzer.service;

import com.stockanalyzer.dto.CompareData;
import com.stockanalyzer.dto.FinancialData;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompareService {

    private final StockService stockService;
    private final FinancialService financialService;

    @Autowired
    public CompareService(StockService stockService, FinancialService financialService) {
        this.stockService = stockService;
        this.financialService = financialService;
    }

    public CompareData compare(List<String> tickers, String period) {
        List<StockDetail> stocks = new ArrayList<>();
        Map<String, List<PriceData>> prices = new LinkedHashMap<>();
        Map<String, List<FinancialData>> financials = new LinkedHashMap<>();

        for (String ticker : tickers) {
            String upper = ticker.toUpperCase();
            stocks.add(stockService.getDetail(upper));
            prices.put(upper, stockService.getPrices(upper, period));
            financials.put(upper, financialService.getFinancials(upper, "annual"));
        }

        return new CompareData(stocks, prices, financials);
    }
}
