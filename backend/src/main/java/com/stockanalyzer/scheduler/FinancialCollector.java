package com.stockanalyzer.scheduler;

import com.stockanalyzer.client.SecEdgarClient;
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

import java.util.List;

@Component
public class FinancialCollector {
    private static final Logger log = LoggerFactory.getLogger(FinancialCollector.class);

    private final SecEdgarClient secEdgarClient;
    private final StockRepository stockRepository;
    private final FinancialRepository financialRepository;

    @Autowired
    public FinancialCollector(SecEdgarClient secEdgarClient,
                              StockRepository stockRepository,
                              FinancialRepository financialRepository) {
        this.secEdgarClient = secEdgarClient;
        this.stockRepository = stockRepository;
        this.financialRepository = financialRepository;
    }

    @Scheduled(cron = "0 0 7 ? * SUN")
    public void collectFinancials() {
        List<Stock> stocks = stockRepository.findAll();
        log.info("Starting financial collection via SEC EDGAR for {} stocks", stocks.size());
        for (Stock stock : stocks) {
            try {
                collectForStock(stock);
                Thread.sleep(200); // SEC EDGAR rate limit: 10 req/sec
            } catch (Exception e) {
                log.error("Failed to collect financials for {}: {}", stock.getTicker(), e.getMessage());
            }
        }
        log.info("Financial collection completed");
    }

    @Transactional
    public void collectForStock(Stock stock) {
        String json = secEdgarClient.fetchCompanyFacts(stock.getTicker());
        if (json == null) {
            log.warn("No SEC EDGAR data for {}", stock.getTicker());
            return;
        }

        List<SecEdgarClient.EdgarFinancial> facts = SecEdgarClient.parseFinancialFacts(json);
        for (SecEdgarClient.EdgarFinancial ef : facts) {
            Financial financial = new Financial();
            financial.setStock(stock);
            financial.setPeriod(String.valueOf(ef.fiscalYear));
            financial.setType("annual");
            financial.setRevenue(ef.revenue);
            financial.setOperatingIncome(ef.operatingIncome);
            financial.setNetIncome(ef.netIncome);
            financial.setTotalAssets(ef.totalAssets);
            financial.setTotalLiabilities(ef.totalLiabilities);
            financial.setTotalEquity(ef.totalEquity);
            financial.setOperatingCashFlow(ef.operatingCashFlow);

            try {
                financialRepository.save(financial);
            } catch (Exception e) {
                log.debug("Skipping duplicate financial for {} period {}: {}",
                    stock.getTicker(), ef.fiscalYear, e.getMessage());
            }
        }
    }
}
