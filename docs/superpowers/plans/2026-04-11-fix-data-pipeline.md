# Fix Data Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 4개 데이터 파이프라인 스케줄러가 실제로 데이터를 수집하도록 수정 — marketCap, 재무제표, 뉴스

**Architecture:** Yahoo v8 chart API (가격/현재가) + SEC EDGAR XBRL API (재무제표, shares outstanding → marketCap 계산) + SEC EDGAR Atom (뉴스). Yahoo v6/v7/v10은 인증 필요로 사용 불가. SEC EDGAR는 User-Agent 헤더만 필요하고 API 키 불필요.

**Tech Stack:** Spring Framework 4.3, Java 11, Hibernate 5.4, PostgreSQL 15, JUnit 4, Mockito 3

---

## File Map

| File | Action | Responsibility |
|------|--------|---------------|
| `SecEdgarClient.java` | Modify | ticker→CIK 매핑 캐시, XBRL 재무 데이터 조회, shares outstanding 조회 추가 |
| `YahooFinanceClient.java` | Modify | `parseQuoteFromChart`에서 `BigDecimal.ZERO` 하드코딩 제거, longName 파싱 개선 |
| `PriceCollector.java` | Modify | 기존 종목 marketCap 갱신 (현재가 × shares outstanding) |
| `FinancialCollector.java` | Modify | Yahoo v10 → SEC EDGAR XBRL로 교체 |
| `NewsCollector.java` | Modify | Finnhub 의존성 제거, SEC EDGAR filings만 사용 |
| `AdminController.java` | Modify | 개별 스케줄러 수동 트리거 엔드포인트 추가 |
| `application.properties` | Modify | SEC EDGAR User-Agent 설정 추가 |

---

### Task 1: SecEdgarClient — ticker→CIK 매핑

**Files:**
- Modify: `backend/src/main/java/com/stockanalyzer/client/SecEdgarClient.java`
- Test: `backend/src/test/java/com/stockanalyzer/client/SecEdgarClientTest.java`

- [ ] **Step 1: Write failing test for CIK lookup**

```java
// SecEdgarClientTest.java — 기존 파일에 추가
@Test
public void resolveCik_returnsCorrectCik() {
    // SEC EDGAR company_tickers.json에서 가져온 실제 데이터
    String mockJson = "{\"0\":{\"cik_str\":320193,\"ticker\":\"AAPL\",\"title\":\"Apple Inc.\"}," +
                       "\"1\":{\"cik_str\":789019,\"ticker\":\"MSFT\",\"title\":\"MICROSOFT CORP\"}}";
    SecEdgarClient client = new SecEdgarClient();
    client.loadTickerMap(mockJson);
    assertEquals("0000320193", client.resolveCik("AAPL"));
    assertEquals("0000789019", client.resolveCik("MSFT"));
    assertNull(client.resolveCik("UNKNOWN"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "com.stockanalyzer.client.SecEdgarClientTest.resolveCik_returnsCorrectCik" 2>&1 | tail -20`
Expected: FAIL — `loadTickerMap` method not found

- [ ] **Step 3: Implement CIK mapping in SecEdgarClient**

Add to `SecEdgarClient.java`:

```java
private static final String TICKER_MAP_URL = "https://www.sec.gov/files/company_tickers.json";
private final Map<String, String> tickerToCik = new ConcurrentHashMap<>();

public void loadTickerMap(String json) {
    try {
        JsonNode root = new ObjectMapper().readTree(json);
        for (JsonNode entry : root) {
            String ticker = entry.path("ticker").asText().toUpperCase();
            int cik = entry.path("cik_str").asInt();
            tickerToCik.put(ticker, String.format("%010d", cik));
        }
        log.info("Loaded {} ticker-to-CIK mappings", tickerToCik.size());
    } catch (Exception e) {
        log.error("Failed to parse ticker map: {}", e.getMessage());
    }
}

public void initTickerMapIfEmpty() {
    if (!tickerToCik.isEmpty()) return;
    try {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", userAgent);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String json = restTemplate.exchange(TICKER_MAP_URL, HttpMethod.GET, entity, String.class).getBody();
        loadTickerMap(json);
    } catch (Exception e) {
        log.error("Failed to fetch ticker map from SEC: {}", e.getMessage());
    }
}

public String resolveCik(String ticker) {
    initTickerMapIfEmpty();
    return tickerToCik.get(ticker.toUpperCase());
}
```

Also add required imports and field:

```java
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Value("${sec.edgar.user-agent:stockanalyzer/1.0 contact@example.com}")
private String userAgent;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "com.stockanalyzer.client.SecEdgarClientTest.resolveCik_returnsCorrectCik" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/stockanalyzer/client/SecEdgarClient.java backend/src/test/java/com/stockanalyzer/client/SecEdgarClientTest.java
git commit -m "feat: add ticker-to-CIK mapping in SecEdgarClient"
```

---

### Task 2: SecEdgarClient — XBRL 재무 데이터 조회

**Files:**
- Modify: `backend/src/main/java/com/stockanalyzer/client/SecEdgarClient.java`
- Test: `backend/src/test/java/com/stockanalyzer/client/SecEdgarClientTest.java`

- [ ] **Step 1: Write failing test for financial facts parsing**

```java
@Test
public void parseFinancialFacts_extractsAnnualData() {
    // Minimal XBRL companyfacts JSON structure
    String json = "{ \"cik\": 320193, \"entityName\": \"Apple Inc.\", \"facts\": { \"us-gaap\": {" +
        "\"RevenueFromContractWithCustomerExcludingAssessedTax\": { \"units\": { \"USD\": [" +
        "  {\"val\": 394328000000, \"fy\": 2024, \"form\": \"10-K\", \"end\": \"2024-09-28\"}," +
        "  {\"val\": 416161000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
        "]}}," +
        "\"NetIncomeLoss\": { \"units\": { \"USD\": [" +
        "  {\"val\": 93736000000, \"fy\": 2024, \"form\": \"10-K\", \"end\": \"2024-09-28\"}," +
        "  {\"val\": 112010000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
        "]}}," +
        "\"OperatingIncomeLoss\": { \"units\": { \"USD\": [" +
        "  {\"val\": 133050000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
        "]}}," +
        "\"Assets\": { \"units\": { \"USD\": [" +
        "  {\"val\": 359241000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
        "]}}," +
        "\"Liabilities\": { \"units\": { \"USD\": [" +
        "  {\"val\": 285508000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
        "]}}," +
        "\"StockholdersEquity\": { \"units\": { \"USD\": [" +
        "  {\"val\": 73733000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
        "]}}," +
        "\"NetCashProvidedByUsedInOperatingActivities\": { \"units\": { \"USD\": [" +
        "  {\"val\": 111482000000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-09-27\"}" +
        "]}}" +
        "}}}";

    List<SecEdgarClient.EdgarFinancial> results = SecEdgarClient.parseFinancialFacts(json);
    assertFalse(results.isEmpty());

    // Latest year
    SecEdgarClient.EdgarFinancial latest = results.get(0);
    assertEquals(2025, latest.fiscalYear);
    assertEquals(new BigDecimal("416161000000"), latest.revenue);
    assertEquals(new BigDecimal("112010000000"), latest.netIncome);
    assertEquals(new BigDecimal("133050000000"), latest.operatingIncome);
    assertEquals(new BigDecimal("359241000000"), latest.totalAssets);
    assertEquals(new BigDecimal("285508000000"), latest.totalLiabilities);
    assertEquals(new BigDecimal("73733000000"), latest.totalEquity);
    assertEquals(new BigDecimal("111482000000"), latest.operatingCashFlow);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "com.stockanalyzer.client.SecEdgarClientTest.parseFinancialFacts_extractsAnnualData" 2>&1 | tail -20`
Expected: FAIL — `EdgarFinancial` and `parseFinancialFacts` not found

- [ ] **Step 3: Implement XBRL parsing**

Add to `SecEdgarClient.java`:

```java
public static class EdgarFinancial {
    public int fiscalYear;
    public String endDate;
    public BigDecimal revenue;
    public BigDecimal netIncome;
    public BigDecimal operatingIncome;
    public BigDecimal totalAssets;
    public BigDecimal totalLiabilities;
    public BigDecimal totalEquity;
    public BigDecimal operatingCashFlow;
}

private static final String XBRL_URL = "https://data.sec.gov/api/xbrl/companyfacts/CIK%s.json";

// Revenue can appear under different concept names
private static final String[] REVENUE_CONCEPTS = {
    "RevenueFromContractWithCustomerExcludingAssessedTax",
    "Revenues",
    "SalesRevenueNet",
    "RevenueFromContractWithCustomerIncludingAssessedTax"
};

private static final String[] CASH_OP_CONCEPTS = {
    "NetCashProvidedByUsedInOperatingActivities",
    "NetCashProvidedByOperatingActivities"
};

public String fetchCompanyFacts(String ticker) {
    String cik = resolveCik(ticker);
    if (cik == null) {
        log.warn("No CIK found for ticker {}", ticker);
        return null;
    }
    String url = String.format(XBRL_URL, cik);
    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", userAgent);
    HttpEntity<String> entity = new HttpEntity<>(headers);
    return restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
}

public static List<EdgarFinancial> parseFinancialFacts(String json) {
    List<EdgarFinancial> results = new ArrayList<>();
    try {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode usGaap = root.path("facts").path("us-gaap");

        // Collect all fiscal years from 10-K filings
        java.util.Set<Integer> fiscalYears = new java.util.TreeSet<>(java.util.Collections.reverseOrder());
        collectFiscalYears(usGaap, "NetIncomeLoss", fiscalYears);
        for (String rc : REVENUE_CONCEPTS) {
            collectFiscalYears(usGaap, rc, fiscalYears);
        }

        for (int fy : fiscalYears) {
            EdgarFinancial ef = new EdgarFinancial();
            ef.fiscalYear = fy;
            ef.netIncome = getAnnualValue(usGaap, "NetIncomeLoss", fy);
            ef.operatingIncome = getAnnualValue(usGaap, "OperatingIncomeLoss", fy);
            ef.totalAssets = getAnnualValue(usGaap, "Assets", fy);
            ef.totalLiabilities = getAnnualValue(usGaap, "Liabilities", fy);
            ef.totalEquity = getAnnualValue(usGaap, "StockholdersEquity", fy);

            // Revenue: try multiple concept names
            for (String rc : REVENUE_CONCEPTS) {
                ef.revenue = getAnnualValue(usGaap, rc, fy);
                if (ef.revenue != null) break;
            }

            // Operating cash flow: try multiple concept names
            for (String cc : CASH_OP_CONCEPTS) {
                ef.operatingCashFlow = getAnnualValue(usGaap, cc, fy);
                if (ef.operatingCashFlow != null) break;
            }

            // Get end date from any available field
            ef.endDate = getAnnualEndDate(usGaap, "NetIncomeLoss", fy);

            // Only include if at least one meaningful field exists
            if (ef.revenue != null || ef.netIncome != null) {
                results.add(ef);
            }
        }
    } catch (Exception e) {
        throw new RuntimeException("Failed to parse XBRL financial facts", e);
    }
    return results;
}

private static void collectFiscalYears(JsonNode usGaap, String concept, java.util.Set<Integer> years) {
    JsonNode entries = usGaap.path(concept).path("units").path("USD");
    if (entries.isMissingNode()) return;
    for (JsonNode entry : entries) {
        if ("10-K".equals(entry.path("form").asText())) {
            years.add(entry.path("fy").asInt());
        }
    }
}

private static BigDecimal getAnnualValue(JsonNode usGaap, String concept, int fiscalYear) {
    JsonNode entries = usGaap.path(concept).path("units").path("USD");
    if (entries.isMissingNode()) return null;
    for (JsonNode entry : entries) {
        if ("10-K".equals(entry.path("form").asText()) && entry.path("fy").asInt() == fiscalYear) {
            return new BigDecimal(String.valueOf(entry.path("val").asLong()));
        }
    }
    return null;
}

private static String getAnnualEndDate(JsonNode usGaap, String concept, int fiscalYear) {
    JsonNode entries = usGaap.path(concept).path("units").path("USD");
    if (entries.isMissingNode()) return null;
    for (JsonNode entry : entries) {
        if ("10-K".equals(entry.path("form").asText()) && entry.path("fy").asInt() == fiscalYear) {
            return entry.path("end").asText(null);
        }
    }
    return null;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "com.stockanalyzer.client.SecEdgarClientTest.parseFinancialFacts_extractsAnnualData" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/stockanalyzer/client/SecEdgarClient.java backend/src/test/java/com/stockanalyzer/client/SecEdgarClientTest.java
git commit -m "feat: add SEC EDGAR XBRL financial data parsing"
```

---

### Task 3: SecEdgarClient — Shares Outstanding 조회

**Files:**
- Modify: `backend/src/main/java/com/stockanalyzer/client/SecEdgarClient.java`
- Test: `backend/src/test/java/com/stockanalyzer/client/SecEdgarClientTest.java`

- [ ] **Step 1: Write failing test**

```java
@Test
public void parseSharesOutstanding_extractsFromXbrl() {
    String json = "{ \"cik\": 320193, \"entityName\": \"Apple\", \"facts\": {" +
        "\"dei\": {" +
        "  \"EntityCommonStockSharesOutstanding\": { \"units\": { \"shares\": [" +
        "    {\"val\": 15115823000, \"fy\": 2024, \"form\": \"10-K\", \"end\": \"2024-10-18\"}," +
        "    {\"val\": 14776353000, \"fy\": 2025, \"form\": \"10-K\", \"end\": \"2025-10-17\"}" +
        "  ]}}" +
        "}, \"us-gaap\": {} }}";

    BigDecimal shares = SecEdgarClient.parseSharesOutstanding(json);
    assertEquals(new BigDecimal("14776353000"), shares);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew test --tests "com.stockanalyzer.client.SecEdgarClientTest.parseSharesOutstanding_extractsFromXbrl" 2>&1 | tail -20`
Expected: FAIL

- [ ] **Step 3: Implement shares outstanding parsing**

Add to `SecEdgarClient.java`:

```java
public static BigDecimal parseSharesOutstanding(String json) {
    try {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        // Primary: dei:EntityCommonStockSharesOutstanding
        JsonNode entries = root.path("facts").path("dei")
            .path("EntityCommonStockSharesOutstanding").path("units").path("shares");
        if (!entries.isMissingNode() && entries.size() > 0) {
            // Get the latest 10-K entry
            JsonNode latest = null;
            for (JsonNode entry : entries) {
                if ("10-K".equals(entry.path("form").asText())) {
                    latest = entry;
                }
            }
            if (latest != null) {
                return new BigDecimal(String.valueOf(latest.path("val").asLong()));
            }
        }
        // Fallback: us-gaap:CommonStockSharesOutstanding
        entries = root.path("facts").path("us-gaap")
            .path("CommonStockSharesOutstanding").path("units").path("shares");
        if (!entries.isMissingNode() && entries.size() > 0) {
            JsonNode latest = null;
            for (JsonNode entry : entries) {
                if ("10-K".equals(entry.path("form").asText())) {
                    latest = entry;
                }
            }
            if (latest != null) {
                return new BigDecimal(String.valueOf(latest.path("val").asLong()));
            }
        }
        return null;
    } catch (Exception e) {
        throw new RuntimeException("Failed to parse shares outstanding", e);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && ./gradlew test --tests "com.stockanalyzer.client.SecEdgarClientTest.parseSharesOutstanding_extractsFromXbrl" 2>&1 | tail -20`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/stockanalyzer/client/SecEdgarClient.java backend/src/test/java/com/stockanalyzer/client/SecEdgarClientTest.java
git commit -m "feat: add shares outstanding parsing from SEC EDGAR"
```

---

### Task 4: PriceCollector — marketCap 갱신

**Files:**
- Modify: `backend/src/main/java/com/stockanalyzer/scheduler/PriceCollector.java`
- Modify: `backend/src/main/java/com/stockanalyzer/client/YahooFinanceClient.java`

- [ ] **Step 1: Fix YahooFinanceClient.parseQuoteFromChart — remove hardcoded ZERO**

In `YahooFinanceClient.java`, change line 107:

```java
// Before:
detail.setMarketCap(BigDecimal.ZERO);

// After: (remove the line entirely — marketCap will be null, calculated separately)
// line removed
```

- [ ] **Step 2: Add SecEdgarClient dependency to PriceCollector and update marketCap logic**

In `PriceCollector.java`:

Add constructor parameter:
```java
private final SecEdgarClient secEdgarClient;

@Autowired
public PriceCollector(YahooFinanceClient yahooClient,
                      StockRepository stockRepository,
                      DailyPriceRepository dailyPriceRepository,
                      SecEdgarClient secEdgarClient) {
    this.yahooClient = yahooClient;
    this.stockRepository = stockRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.secEdgarClient = secEdgarClient;
}
```

Modify `collectForTicker()` to always update marketCap:

```java
@Transactional
public void collectForTicker(String ticker) {
    Stock stock = stockRepository.findByTicker(ticker);
    if (stock == null) {
        StockDetail detail = yahooClient.fetchQuote(ticker);
        stock = new Stock();
        stock.setTicker(detail.getTicker());
        stock.setCompanyName(detail.getCompanyName());
        stock.setExchange(detail.getExchange());
        stock = stockRepository.save(stock);
    }

    // Update marketCap: currentPrice * sharesOutstanding
    try {
        StockDetail quote = yahooClient.fetchQuote(ticker);
        BigDecimal currentPrice = quote.getCurrentPrice();
        if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            String factsJson = secEdgarClient.fetchCompanyFacts(ticker);
            if (factsJson != null) {
                BigDecimal shares = SecEdgarClient.parseSharesOutstanding(factsJson);
                if (shares != null) {
                    stock.setMarketCap(currentPrice.multiply(shares));
                    stockRepository.save(stock);
                }
            }
        }
    } catch (Exception e) {
        log.warn("Failed to update marketCap for {}: {}", ticker, e.getMessage());
    }

    List<PriceData> prices = yahooClient.fetchPriceHistory(ticker, "1mo", "1d");
    Stock finalStock = stock;
    List<DailyPrice> entities = prices.stream().map(pd -> {
        DailyPrice dp = new DailyPrice();
        dp.setStock(finalStock);
        dp.setDate(pd.getDate());
        dp.setOpen(pd.getOpen());
        dp.setHigh(pd.getHigh());
        dp.setLow(pd.getLow());
        dp.setClose(pd.getClose());
        dp.setAdjustedClose(pd.getAdjustedClose());
        dp.setVolume(pd.getVolume());
        return dp;
    }).collect(Collectors.toList());
    dailyPriceRepository.saveAll(entities);
    log.info("Collected {} price records for {}", entities.size(), ticker);
}
```

Add import:
```java
import com.stockanalyzer.client.SecEdgarClient;
import java.math.BigDecimal;
```

- [ ] **Step 3: Run full test suite to check for regressions**

Run: `cd backend && ./gradlew test 2>&1 | tail -30`
Expected: All tests PASS (existing PriceCollector tests may need SecEdgarClient mock added)

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/stockanalyzer/scheduler/PriceCollector.java backend/src/main/java/com/stockanalyzer/client/YahooFinanceClient.java
git commit -m "feat: update marketCap using currentPrice * sharesOutstanding from SEC EDGAR"
```

---

### Task 5: FinancialCollector — Yahoo → SEC EDGAR 교체

**Files:**
- Modify: `backend/src/main/java/com/stockanalyzer/scheduler/FinancialCollector.java`

- [ ] **Step 1: Replace Yahoo v10 with SEC EDGAR XBRL**

Rewrite `FinancialCollector.java`:

```java
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
```

- [ ] **Step 2: Run tests**

Run: `cd backend && ./gradlew test 2>&1 | tail -30`
Expected: PASS (FinancialCollector tests may need updates for new constructor)

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/stockanalyzer/scheduler/FinancialCollector.java
git commit -m "feat: replace Yahoo v10 with SEC EDGAR XBRL for financial data collection"
```

---

### Task 6: NewsCollector — Finnhub 제거, SEC EDGAR filings만 사용

**Files:**
- Modify: `backend/src/main/java/com/stockanalyzer/scheduler/NewsCollector.java`

- [ ] **Step 1: Remove Finnhub dependency from NewsCollector**

Rewrite `NewsCollector.java`:

```java
package com.stockanalyzer.scheduler;

import com.stockanalyzer.client.SecEdgarClient;
import com.stockanalyzer.dto.NewsItem;
import com.stockanalyzer.entity.News;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.entity.StockNews;
import com.stockanalyzer.repository.NewsRepository;
import com.stockanalyzer.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class NewsCollector {
    private static final Logger log = LoggerFactory.getLogger(NewsCollector.class);

    private final StockRepository stockRepository;
    private final NewsRepository newsRepository;
    private final SecEdgarClient secEdgarClient;

    @Autowired
    public NewsCollector(StockRepository stockRepository, NewsRepository newsRepository,
                         SecEdgarClient secEdgarClient) {
        this.stockRepository = stockRepository;
        this.newsRepository = newsRepository;
        this.secEdgarClient = secEdgarClient;
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void collectNews() {
        List<Stock> stocks = stockRepository.findAll();
        log.info("Starting news collection (SEC EDGAR filings) for {} stocks", stocks.size());

        for (Stock stock : stocks) {
            try {
                collectEdgarFilings(stock);
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("Failed to collect news for {}: {}", stock.getTicker(), e.getMessage());
            }
        }
        log.info("News collection completed");
    }

    @Transactional
    public void collectEdgarFilings(Stock stock) {
        List<NewsItem> items = secEdgarClient.fetchFilings(stock.getTicker(), 10);
        for (NewsItem item : items) {
            News news = toNewsEntity(item, "SEC EDGAR");
            news = newsRepository.save(news);
            StockNews sn = new StockNews();
            sn.setStock(stock);
            sn.setNews(news);
            try {
                newsRepository.saveStockNews(sn);
            } catch (Exception e) {
                log.debug("Duplicate stock_news skipped for {} / news {}", stock.getTicker(), news.getId());
            }
        }
    }

    private News toNewsEntity(NewsItem item, String source) {
        News news = new News();
        news.setTitle(item.getTitle() != null ? item.getTitle() : "(no title)");
        news.setSource(source);
        news.setUrl(item.getUrl() != null ? item.getUrl() : "");
        news.setPublishedAt(parseDateTime(item.getPublishedAt()));
        news.setSummary(item.getSummary());
        news.setImageUrl(item.getImageUrl());
        return news;
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isEmpty()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(s.replace("Z", "").replace("T", "T"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd backend && ./gradlew test 2>&1 | tail -30`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/stockanalyzer/scheduler/NewsCollector.java
git commit -m "feat: replace Finnhub with SEC EDGAR filings for news collection"
```

---

### Task 7: AdminController — 개별 수동 트리거 엔드포인트

**Files:**
- Modify: `backend/src/main/java/com/stockanalyzer/controller/AdminController.java`

- [ ] **Step 1: Add NewsCollector dependency and individual trigger endpoints**

Add to `AdminController.java`:

Constructor에 `NewsCollector` 추가:
```java
private final NewsCollector newsCollector;

@Autowired
public AdminController(PriceCollector priceCollector,
                       FinancialCollector financialCollector,
                       MetricsCalculator metricsCalculator,
                       NewsCollector newsCollector,
                       DataSource dataSource) {
    this.priceCollector = priceCollector;
    this.financialCollector = financialCollector;
    this.metricsCalculator = metricsCalculator;
    this.newsCollector = newsCollector;
    this.dataSource = dataSource;
}
```

Add import:
```java
import com.stockanalyzer.scheduler.NewsCollector;
```

Add endpoints:
```java
@GetMapping("/collect-prices")
public ApiResponse<String> collectPrices() {
    new Thread(() -> {
        try {
            priceCollector.collectDailyPrices();
        } catch (Exception e) {
            log.error("Manual price collection failed: {}", e.getMessage(), e);
        }
    }).start();
    return ApiResponse.ok("Price collection started");
}

@GetMapping("/collect-financials")
public ApiResponse<String> collectFinancials() {
    new Thread(() -> {
        try {
            financialCollector.collectFinancials();
        } catch (Exception e) {
            log.error("Manual financial collection failed: {}", e.getMessage(), e);
        }
    }).start();
    return ApiResponse.ok("Financial collection started");
}

@GetMapping("/calculate-metrics")
public ApiResponse<String> calculateMetrics() {
    new Thread(() -> {
        try {
            metricsCalculator.run();
        } catch (Exception e) {
            log.error("Manual metrics calculation failed: {}", e.getMessage(), e);
        }
    }).start();
    return ApiResponse.ok("Metrics calculation started");
}

@GetMapping("/collect-news")
public ApiResponse<String> collectNews() {
    new Thread(() -> {
        try {
            newsCollector.collectNews();
        } catch (Exception e) {
            log.error("Manual news collection failed: {}", e.getMessage(), e);
        }
    }).start();
    return ApiResponse.ok("News collection started");
}
```

- [ ] **Step 2: Run tests**

Run: `cd backend && ./gradlew test 2>&1 | tail -30`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/stockanalyzer/controller/AdminController.java
git commit -m "feat: add individual manual trigger endpoints for each scheduler"
```

---

### Task 8: application.properties — SEC EDGAR 설정 추가

**Files:**
- Modify: `backend/src/main/resources/application.properties`

- [ ] **Step 1: Add SEC EDGAR user-agent config**

Add to `application.properties`:
```properties
sec.edgar.user-agent=stockanalyzer/1.0 contact@example.com
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/resources/application.properties
git commit -m "feat: add SEC EDGAR user-agent configuration"
```

---

### Task 9: Integration Test — 수동 트리거로 전체 파이프라인 검증

- [ ] **Step 1: Build and verify compilation**

Run: `cd backend && ./gradlew build -x test 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run full test suite**

Run: `cd backend && ./gradlew test 2>&1 | tail -30`
Expected: All tests PASS

- [ ] **Step 3: Deploy and test manually**

배포 후 순서대로 호출:
```bash
# 1. 가격 + marketCap 수집
curl http://fumarole.synology.me:39091/api/admin/collect-prices
# 2. 재무제표 수집
curl http://fumarole.synology.me:39091/api/admin/collect-financials
# 3. 지표 계산
curl http://fumarole.synology.me:39091/api/admin/calculate-metrics
# 4. 뉴스 수집
curl http://fumarole.synology.me:39091/api/admin/collect-news
```

검증:
```bash
# marketCap이 0이 아닌지 확인
curl "http://fumarole.synology.me:39091/api/screening?page=0&size=3"
# 기대: marketCap > 0, per/pbr/roe != null
```

- [ ] **Step 4: Final commit if any fixes needed**
