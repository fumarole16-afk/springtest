package com.stockanalyzer.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockanalyzer.client.YahooFinanceClient;
import com.stockanalyzer.entity.Financial;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.repository.FinancialRepository;
import com.stockanalyzer.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FinancialCollector {
    private static final Logger log = LoggerFactory.getLogger(FinancialCollector.class);

    private final YahooFinanceClient yahooClient;
    private final StockRepository stockRepository;
    private final FinancialRepository financialRepository;

    @Autowired
    public FinancialCollector(YahooFinanceClient yahooClient,
                              StockRepository stockRepository,
                              FinancialRepository financialRepository) {
        this.yahooClient = yahooClient;
        this.stockRepository = stockRepository;
        this.financialRepository = financialRepository;
    }

    @Scheduled(cron = "0 0 7 ? * SUN")
    public void collectFinancials() {
        List<Stock> stocks = stockRepository.findAll();
        log.info("Starting financial collection for {} stocks", stocks.size());
        for (Stock stock : stocks) {
            try {
                collectForStock(stock);
                Thread.sleep(1200);
            } catch (Exception e) {
                log.error("Failed to collect financials for {}: {}", stock.getTicker(), e.getMessage());
            }
        }
        log.info("Financial collection completed");
    }

    @Transactional
    public void collectForStock(Stock stock) {
        JsonNode root = yahooClient.fetchFinancials(stock.getTicker());
        JsonNode result = root.path("quoteSummary").path("result").get(0);
        if (result == null || result.isMissingNode()) {
            log.warn("No financial data for {}", stock.getTicker());
            return;
        }

        parseAndSaveIncomeStatements(stock, result);
    }

    private void parseAndSaveIncomeStatements(Stock stock, JsonNode result) {
        JsonNode statements = result.path("incomeStatementHistory").path("incomeStatementHistory");
        JsonNode balanceSheets = result.path("balanceSheetHistory").path("balanceSheetStatements");
        JsonNode cashflows = result.path("cashflowStatementHistory").path("cashflowStatements");

        for (int i = 0; i < statements.size(); i++) {
            JsonNode income = statements.get(i);
            JsonNode balance = balanceSheets.size() > i ? balanceSheets.get(i) : null;
            JsonNode cashflow = cashflows.size() > i ? cashflows.get(i) : null;

            String endDate = income.path("endDate").path("fmt").asText();
            if (endDate == null || endDate.isEmpty()) continue;

            // Period format: "2023" for annual
            String period = endDate.length() >= 4 ? endDate.substring(0, 4) : endDate;

            Financial financial = new Financial();
            financial.setStock(stock);
            financial.setPeriod(period);
            financial.setType("annual");
            financial.setRevenue(rawValue(income.path("totalRevenue")));
            financial.setOperatingIncome(rawValue(income.path("operatingIncome")));
            financial.setNetIncome(rawValue(income.path("netIncome")));

            if (balance != null) {
                financial.setTotalAssets(rawValue(balance.path("totalAssets")));
                financial.setTotalLiabilities(rawValue(balance.path("totalLiab")));
                financial.setTotalEquity(rawValue(balance.path("totalStockholderEquity")));
            }

            if (cashflow != null) {
                financial.setOperatingCashFlow(rawValue(cashflow.path("totalCashFromOperatingActivities")));
            }

            try {
                financialRepository.save(financial);
            } catch (Exception e) {
                log.debug("Skipping duplicate financial record for {} period {}: {}", stock.getTicker(), period, e.getMessage());
            }
        }
    }

    private BigDecimal rawValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        JsonNode raw = node.path("raw");
        if (!raw.isMissingNode() && !raw.isNull()) {
            return new BigDecimal(raw.asText());
        }
        return null;
    }
}
