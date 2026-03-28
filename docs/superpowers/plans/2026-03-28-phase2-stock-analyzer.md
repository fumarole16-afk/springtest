# Phase 2: Screening + Financials + Sector Analysis Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 스크리닝(12가지 필터), 재무제표(분석형), 섹터/산업 분석 기능을 기존 Phase 1 인프라 위에 추가한다.

**Architecture:** 4개의 새 엔티티(Financial, StockMetric, SectorMetric, IndustryMetric)를 추가하고, JPA Criteria API로 동적 스크리닝 쿼리를 구성한다. 두 개의 새 스케줄러가 재무데이터 수집과 지표 계산을 담당한다. 프론트엔드는 Recharts Treemap으로 섹터 히트맵, 필터 패널로 스크리닝, 탭 UI로 재무제표를 표시한다.

**Tech Stack:** Spring MVC 4.3.x, JPA Criteria API, Recharts (Treemap, BarChart, LineChart), 기존 Phase 1 스택 전체

---

## File Structure

### Backend — 새로 추가

```
backend/src/main/java/com/stockanalyzer/
├── entity/
│   ├── Financial.java
│   ├── StockMetric.java
│   ├── SectorMetric.java
│   └── IndustryMetric.java
├── repository/
│   ├── FinancialRepository.java
│   ├── StockMetricRepository.java
│   ├── SectorMetricRepository.java
│   └── IndustryMetricRepository.java
├── dto/
│   ├── FinancialData.java
│   ├── FinancialRatios.java
│   ├── ScreeningFilter.java
│   ├── ScreeningResult.java
│   ├── SectorOverview.java
│   ├── SectorDetail.java
│   ├── SectorPerformance.java
│   └── IndustryDetail.java
├── service/
│   ├── FinancialService.java
│   ├── ScreeningService.java
│   └── SectorService.java
├── controller/
│   ├── ScreeningController.java
│   └── SectorController.java
└── scheduler/
    ├── FinancialCollector.java
    └── MetricsCalculator.java
```

### Backend — 수정

```
backend/src/main/java/com/stockanalyzer/
├── controller/StockController.java        # financials 엔드포인트 추가
```

### Frontend — 새로 추가

```
frontend/src/
├── components/
│   ├── FilterPanel.tsx
│   ├── SectorHeatmap.tsx
│   ├── PerformanceChart.tsx
│   ├── FinancialTable.tsx
│   └── RatioComparison.tsx
└── pages/
    ├── Screening.tsx
    ├── SectorAnalysis.tsx
    └── IndustryDetail.tsx
```

### Frontend — 수정

```
frontend/src/
├── api/stockApi.ts        # 새 API 함수 추가
├── App.tsx                # 라우트 추가
└── pages/StockDetail.tsx  # 재무제표 탭 추가
```

---

## Task 1: Financial Entity + Repository

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/entity/Financial.java`
- Create: `backend/src/main/java/com/stockanalyzer/repository/FinancialRepository.java`
- Test: `backend/src/test/java/com/stockanalyzer/repository/FinancialRepositoryTest.java`

- [ ] **Step 1: 테스트 작성**

```java
package com.stockanalyzer.repository;

import com.stockanalyzer.entity.Financial;
import com.stockanalyzer.entity.Industry;
import com.stockanalyzer.entity.Sector;
import com.stockanalyzer.entity.Stock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class FinancialRepositoryTest {

    @Autowired private FinancialRepository financialRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private IndustryRepository industryRepository;

    private Stock apple;

    @Before
    public void setUp() {
        Sector tech = sectorRepository.save(new Sector("Technology", "Tech"));
        Industry sw = industryRepository.save(new Industry("Software", "SW", tech));
        apple = new Stock();
        apple.setTicker("AAPL");
        apple.setCompanyName("Apple Inc.");
        apple.setExchange("NASDAQ");
        apple.setMarketCap(new BigDecimal("3000000000000"));
        apple.setIndustry(sw);
        apple = stockRepository.save(apple);

        Financial f1 = new Financial();
        f1.setStock(apple);
        f1.setPeriod("2025");
        f1.setType("annual");
        f1.setRevenue(new BigDecimal("394328000000"));
        f1.setOperatingIncome(new BigDecimal("114301000000"));
        f1.setNetIncome(new BigDecimal("96995000000"));
        f1.setTotalAssets(new BigDecimal("352583000000"));
        f1.setTotalLiabilities(new BigDecimal("290437000000"));
        f1.setTotalEquity(new BigDecimal("62146000000"));
        f1.setOperatingCashFlow(new BigDecimal("110543000000"));
        financialRepository.save(f1);

        Financial f2 = new Financial();
        f2.setStock(apple);
        f2.setPeriod("2024");
        f2.setType("annual");
        f2.setRevenue(new BigDecimal("365000000000"));
        f2.setOperatingIncome(new BigDecimal("105000000000"));
        f2.setNetIncome(new BigDecimal("90000000000"));
        f2.setTotalAssets(new BigDecimal("340000000000"));
        f2.setTotalLiabilities(new BigDecimal("280000000000"));
        f2.setTotalEquity(new BigDecimal("60000000000"));
        f2.setOperatingCashFlow(new BigDecimal("100000000000"));
        financialRepository.save(f2);
    }

    @Test
    public void findByStockAndType_returnsAnnualFinancials() {
        List<Financial> results = financialRepository.findByStockAndType(apple.getId(), "annual");
        assertEquals(2, results.size());
        assertEquals("2025", results.get(0).getPeriod());
    }

    @Test
    public void findLatestByStock_returnsNewestPeriod() {
        Financial latest = financialRepository.findLatestByStock(apple.getId(), "annual");
        assertNotNull(latest);
        assertEquals("2025", latest.getPeriod());
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

Run: `cd backend && ./gradlew test --tests '*FinancialRepositoryTest'`
Expected: FAIL — Financial 클래스가 존재하지 않음

- [ ] **Step 3: Financial Entity 작성**

```java
package com.stockanalyzer.entity;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "financials",
       uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "period"}))
public class Financial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false, length = 10)
    private String period;

    @Column(nullable = false, length = 10)
    private String type;

    @Column(precision = 20, scale = 2)
    private BigDecimal revenue;

    @Column(name = "operating_income", precision = 20, scale = 2)
    private BigDecimal operatingIncome;

    @Column(name = "net_income", precision = 20, scale = 2)
    private BigDecimal netIncome;

    @Column(name = "total_assets", precision = 20, scale = 2)
    private BigDecimal totalAssets;

    @Column(name = "total_liabilities", precision = 20, scale = 2)
    private BigDecimal totalLiabilities;

    @Column(name = "total_equity", precision = 20, scale = 2)
    private BigDecimal totalEquity;

    @Column(name = "operating_cash_flow", precision = 20, scale = 2)
    private BigDecimal operatingCashFlow;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    public Financial() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    public BigDecimal getOperatingIncome() { return operatingIncome; }
    public void setOperatingIncome(BigDecimal operatingIncome) { this.operatingIncome = operatingIncome; }
    public BigDecimal getNetIncome() { return netIncome; }
    public void setNetIncome(BigDecimal netIncome) { this.netIncome = netIncome; }
    public BigDecimal getTotalAssets() { return totalAssets; }
    public void setTotalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; }
    public BigDecimal getTotalLiabilities() { return totalLiabilities; }
    public void setTotalLiabilities(BigDecimal totalLiabilities) { this.totalLiabilities = totalLiabilities; }
    public BigDecimal getTotalEquity() { return totalEquity; }
    public void setTotalEquity(BigDecimal totalEquity) { this.totalEquity = totalEquity; }
    public BigDecimal getOperatingCashFlow() { return operatingCashFlow; }
    public void setOperatingCashFlow(BigDecimal operatingCashFlow) { this.operatingCashFlow = operatingCashFlow; }
    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }
}
```

- [ ] **Step 4: FinancialRepository 작성**

```java
package com.stockanalyzer.repository;

import com.stockanalyzer.entity.Financial;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class FinancialRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Financial> findByStockAndType(Long stockId, String type) {
        return em.createQuery(
                "SELECT f FROM Financial f WHERE f.stock.id = :stockId AND f.type = :type ORDER BY f.period DESC",
                Financial.class)
                .setParameter("stockId", stockId)
                .setParameter("type", type)
                .getResultList();
    }

    public Financial findLatestByStock(Long stockId, String type) {
        List<Financial> results = em.createQuery(
                "SELECT f FROM Financial f WHERE f.stock.id = :stockId AND f.type = :type ORDER BY f.period DESC",
                Financial.class)
                .setParameter("stockId", stockId)
                .setParameter("type", type)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public List<Financial> findTwoLatestAnnual(Long stockId) {
        return em.createQuery(
                "SELECT f FROM Financial f WHERE f.stock.id = :stockId AND f.type = 'annual' ORDER BY f.period DESC",
                Financial.class)
                .setParameter("stockId", stockId)
                .setMaxResults(2)
                .getResultList();
    }

    @Transactional
    public Financial save(Financial financial) {
        if (financial.getId() == null) { em.persist(financial); return financial; }
        return em.merge(financial);
    }
}
```

- [ ] **Step 5: 테스트 실행 — 성공 확인**

Run: `cd backend && ./gradlew test --tests '*FinancialRepositoryTest'`
Expected: All 2 tests PASS

- [ ] **Step 6: 커밋**

```bash
git add backend/src/
git commit -m "feat: add Financial entity and repository with tests"
```

---

## Task 2: StockMetric Entity + Repository (Criteria API)

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/entity/StockMetric.java`
- Create: `backend/src/main/java/com/stockanalyzer/dto/ScreeningFilter.java`
- Create: `backend/src/main/java/com/stockanalyzer/repository/StockMetricRepository.java`
- Test: `backend/src/test/java/com/stockanalyzer/repository/StockMetricRepositoryTest.java`

- [ ] **Step 1: 테스트 작성**

```java
package com.stockanalyzer.repository;

import com.stockanalyzer.dto.ScreeningFilter;
import com.stockanalyzer.entity.Industry;
import com.stockanalyzer.entity.Sector;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.entity.StockMetric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class StockMetricRepositoryTest {

    @Autowired private StockMetricRepository stockMetricRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private IndustryRepository industryRepository;

    private Sector tech;

    @Before
    public void setUp() {
        tech = sectorRepository.save(new Sector("Technology", "Tech"));
        Industry sw = industryRepository.save(new Industry("Software", "SW", tech));

        Stock apple = new Stock();
        apple.setTicker("AAPL");
        apple.setCompanyName("Apple Inc.");
        apple.setExchange("NASDAQ");
        apple.setMarketCap(new BigDecimal("3000000000000"));
        apple.setIndustry(sw);
        apple = stockRepository.save(apple);

        StockMetric m1 = new StockMetric();
        m1.setStock(apple);
        m1.setDate(LocalDate.of(2026, 3, 28));
        m1.setPer(new BigDecimal("28.5"));
        m1.setPbr(new BigDecimal("45.2"));
        m1.setDividendYield(new BigDecimal("0.55"));
        m1.setRoe(new BigDecimal("0.285"));
        m1.setDebtRatio(new BigDecimal("4.67"));
        m1.setRevenueGrowth(new BigDecimal("0.08"));
        m1.setOperatingMargin(new BigDecimal("0.29"));
        m1.setWeek52High(new BigDecimal("199.62"));
        m1.setWeek52Low(new BigDecimal("140.00"));
        m1.setAvgVolume30d(55000000L);
        stockMetricRepository.save(m1);

        Stock msft = new Stock();
        msft.setTicker("MSFT");
        msft.setCompanyName("Microsoft Corporation");
        msft.setExchange("NASDAQ");
        msft.setMarketCap(new BigDecimal("2800000000000"));
        msft.setIndustry(sw);
        msft = stockRepository.save(msft);

        StockMetric m2 = new StockMetric();
        m2.setStock(msft);
        m2.setDate(LocalDate.of(2026, 3, 28));
        m2.setPer(new BigDecimal("35.0"));
        m2.setPbr(new BigDecimal("12.0"));
        m2.setDividendYield(new BigDecimal("0.72"));
        m2.setRoe(new BigDecimal("0.38"));
        m2.setDebtRatio(new BigDecimal("1.50"));
        m2.setRevenueGrowth(new BigDecimal("0.15"));
        m2.setOperatingMargin(new BigDecimal("0.42"));
        m2.setWeek52High(new BigDecimal("420.00"));
        m2.setWeek52Low(new BigDecimal("310.00"));
        m2.setAvgVolume30d(25000000L);
        stockMetricRepository.save(m2);
    }

    @Test
    public void findByFilters_noFilters_returnsAll() {
        ScreeningFilter filter = new ScreeningFilter();
        List<StockMetric> results = stockMetricRepository.findByFilters(filter, 0, 20, "marketCap,desc");
        assertEquals(2, results.size());
    }

    @Test
    public void findByFilters_maxPer30_returnsOnlyApple() {
        ScreeningFilter filter = new ScreeningFilter();
        filter.setMaxPer(new BigDecimal("30"));
        List<StockMetric> results = stockMetricRepository.findByFilters(filter, 0, 20, "marketCap,desc");
        assertEquals(1, results.size());
        assertEquals("AAPL", results.get(0).getStock().getTicker());
    }

    @Test
    public void findByFilters_minRoe035_returnsOnlyMsft() {
        ScreeningFilter filter = new ScreeningFilter();
        filter.setMinRoe(new BigDecimal("0.35"));
        List<StockMetric> results = stockMetricRepository.findByFilters(filter, 0, 20, "marketCap,desc");
        assertEquals(1, results.size());
        assertEquals("MSFT", results.get(0).getStock().getTicker());
    }

    @Test
    public void countByFilters_returnsCorrectCount() {
        ScreeningFilter filter = new ScreeningFilter();
        long count = stockMetricRepository.countByFilters(filter);
        assertEquals(2, count);
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

Run: `cd backend && ./gradlew test --tests '*StockMetricRepositoryTest'`
Expected: FAIL

- [ ] **Step 3: ScreeningFilter DTO 작성**

```java
package com.stockanalyzer.dto;

import java.math.BigDecimal;

public class ScreeningFilter {
    private Long sectorId;
    private BigDecimal minPer;
    private BigDecimal maxPer;
    private BigDecimal minPbr;
    private BigDecimal maxPbr;
    private BigDecimal minCap;
    private BigDecimal maxCap;
    private BigDecimal minDividendYield;
    private BigDecimal minRoe;
    private BigDecimal maxDebtRatio;
    private BigDecimal minRevenueGrowth;
    private BigDecimal minOperatingMargin;
    private String exchange;

    public Long getSectorId() { return sectorId; }
    public void setSectorId(Long sectorId) { this.sectorId = sectorId; }
    public BigDecimal getMinPer() { return minPer; }
    public void setMinPer(BigDecimal minPer) { this.minPer = minPer; }
    public BigDecimal getMaxPer() { return maxPer; }
    public void setMaxPer(BigDecimal maxPer) { this.maxPer = maxPer; }
    public BigDecimal getMinPbr() { return minPbr; }
    public void setMinPbr(BigDecimal minPbr) { this.minPbr = minPbr; }
    public BigDecimal getMaxPbr() { return maxPbr; }
    public void setMaxPbr(BigDecimal maxPbr) { this.maxPbr = maxPbr; }
    public BigDecimal getMinCap() { return minCap; }
    public void setMinCap(BigDecimal minCap) { this.minCap = minCap; }
    public BigDecimal getMaxCap() { return maxCap; }
    public void setMaxCap(BigDecimal maxCap) { this.maxCap = maxCap; }
    public BigDecimal getMinDividendYield() { return minDividendYield; }
    public void setMinDividendYield(BigDecimal minDividendYield) { this.minDividendYield = minDividendYield; }
    public BigDecimal getMinRoe() { return minRoe; }
    public void setMinRoe(BigDecimal minRoe) { this.minRoe = minRoe; }
    public BigDecimal getMaxDebtRatio() { return maxDebtRatio; }
    public void setMaxDebtRatio(BigDecimal maxDebtRatio) { this.maxDebtRatio = maxDebtRatio; }
    public BigDecimal getMinRevenueGrowth() { return minRevenueGrowth; }
    public void setMinRevenueGrowth(BigDecimal minRevenueGrowth) { this.minRevenueGrowth = minRevenueGrowth; }
    public BigDecimal getMinOperatingMargin() { return minOperatingMargin; }
    public void setMinOperatingMargin(BigDecimal minOperatingMargin) { this.minOperatingMargin = minOperatingMargin; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
}
```

- [ ] **Step 4: StockMetric Entity 작성**

```java
package com.stockanalyzer.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stock_metrics",
       uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "date"}),
       indexes = {
           @Index(columnList = "stock_id, date"),
           @Index(columnList = "per"),
           @Index(columnList = "pbr"),
           @Index(columnList = "roe"),
           @Index(columnList = "debt_ratio")
       })
public class StockMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private LocalDate date;

    @Column(precision = 10, scale = 4)
    private BigDecimal per;

    @Column(precision = 10, scale = 4)
    private BigDecimal pbr;

    @Column(name = "dividend_yield", precision = 10, scale = 4)
    private BigDecimal dividendYield;

    @Column(name = "week52_high", precision = 12, scale = 4)
    private BigDecimal week52High;

    @Column(name = "week52_low", precision = 12, scale = 4)
    private BigDecimal week52Low;

    @Column(name = "avg_volume_30d")
    private Long avgVolume30d;

    @Column(name = "revenue_growth", precision = 10, scale = 4)
    private BigDecimal revenueGrowth;

    @Column(name = "operating_margin", precision = 10, scale = 4)
    private BigDecimal operatingMargin;

    @Column(precision = 10, scale = 4)
    private BigDecimal roe;

    @Column(name = "debt_ratio", precision = 10, scale = 4)
    private BigDecimal debtRatio;

    public StockMetric() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getPer() { return per; }
    public void setPer(BigDecimal per) { this.per = per; }
    public BigDecimal getPbr() { return pbr; }
    public void setPbr(BigDecimal pbr) { this.pbr = pbr; }
    public BigDecimal getDividendYield() { return dividendYield; }
    public void setDividendYield(BigDecimal dividendYield) { this.dividendYield = dividendYield; }
    public BigDecimal getWeek52High() { return week52High; }
    public void setWeek52High(BigDecimal week52High) { this.week52High = week52High; }
    public BigDecimal getWeek52Low() { return week52Low; }
    public void setWeek52Low(BigDecimal week52Low) { this.week52Low = week52Low; }
    public Long getAvgVolume30d() { return avgVolume30d; }
    public void setAvgVolume30d(Long avgVolume30d) { this.avgVolume30d = avgVolume30d; }
    public BigDecimal getRevenueGrowth() { return revenueGrowth; }
    public void setRevenueGrowth(BigDecimal revenueGrowth) { this.revenueGrowth = revenueGrowth; }
    public BigDecimal getOperatingMargin() { return operatingMargin; }
    public void setOperatingMargin(BigDecimal operatingMargin) { this.operatingMargin = operatingMargin; }
    public BigDecimal getRoe() { return roe; }
    public void setRoe(BigDecimal roe) { this.roe = roe; }
    public BigDecimal getDebtRatio() { return debtRatio; }
    public void setDebtRatio(BigDecimal debtRatio) { this.debtRatio = debtRatio; }
}
```

- [ ] **Step 5: StockMetricRepository 작성 (Criteria API)**

```java
package com.stockanalyzer.repository;

import com.stockanalyzer.dto.ScreeningFilter;
import com.stockanalyzer.entity.StockMetric;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class StockMetricRepository {

    @PersistenceContext
    private EntityManager em;

    public List<StockMetric> findByFilters(ScreeningFilter filter, int page, int size, String sort) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<StockMetric> cq = cb.createQuery(StockMetric.class);
        Root<StockMetric> root = cq.from(StockMetric.class);
        root.fetch("stock", JoinType.LEFT);

        List<Predicate> predicates = buildPredicates(cb, root, filter);
        cq.where(predicates.toArray(new Predicate[0]));

        applySorting(cb, cq, root, sort);

        return em.createQuery(cq)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public long countByFilters(ScreeningFilter filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<StockMetric> root = cq.from(StockMetric.class);
        cq.select(cb.count(root));

        List<Predicate> predicates = buildPredicates(cb, root, filter);
        cq.where(predicates.toArray(new Predicate[0]));

        return em.createQuery(cq).getSingleResult();
    }

    public LocalDate getLatestDate() {
        List<LocalDate> results = em.createQuery(
                "SELECT MAX(sm.date) FROM StockMetric sm", LocalDate.class)
                .getResultList();
        return results.isEmpty() || results.get(0) == null ? LocalDate.now() : results.get(0);
    }

    @Transactional
    public StockMetric save(StockMetric metric) {
        if (metric.getId() == null) { em.persist(metric); return metric; }
        return em.merge(metric);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<StockMetric> root, ScreeningFilter filter) {
        List<Predicate> predicates = new ArrayList<>();

        LocalDate latestDate = getLatestDate();
        predicates.add(cb.equal(root.get("date"), latestDate));

        if (filter.getSectorId() != null) {
            predicates.add(cb.equal(
                root.get("stock").get("industry").get("sector").get("id"), filter.getSectorId()));
        }
        if (filter.getExchange() != null) {
            predicates.add(cb.equal(root.get("stock").get("exchange"), filter.getExchange()));
        }
        if (filter.getMinPer() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("per"), filter.getMinPer()));
        }
        if (filter.getMaxPer() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("per"), filter.getMaxPer()));
        }
        if (filter.getMinPbr() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("pbr"), filter.getMinPbr()));
        }
        if (filter.getMaxPbr() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("pbr"), filter.getMaxPbr()));
        }
        if (filter.getMinCap() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("stock").get("marketCap"), filter.getMinCap()));
        }
        if (filter.getMaxCap() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("stock").get("marketCap"), filter.getMaxCap()));
        }
        if (filter.getMinDividendYield() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dividendYield"), filter.getMinDividendYield()));
        }
        if (filter.getMinRoe() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("roe"), filter.getMinRoe()));
        }
        if (filter.getMaxDebtRatio() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("debtRatio"), filter.getMaxDebtRatio()));
        }
        if (filter.getMinRevenueGrowth() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("revenueGrowth"), filter.getMinRevenueGrowth()));
        }
        if (filter.getMinOperatingMargin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("operatingMargin"), filter.getMinOperatingMargin()));
        }

        return predicates;
    }

    private void applySorting(CriteriaBuilder cb, CriteriaQuery<StockMetric> cq, Root<StockMetric> root, String sort) {
        if (sort == null || sort.isEmpty()) {
            cq.orderBy(cb.desc(root.get("stock").get("marketCap")));
            return;
        }
        String[] parts = sort.split(",");
        String field = parts[0];
        boolean desc = parts.length > 1 && "desc".equalsIgnoreCase(parts[1]);

        Path<?> path;
        if ("marketCap".equals(field)) {
            path = root.get("stock").get("marketCap");
        } else {
            path = root.get(field);
        }
        cq.orderBy(desc ? cb.desc(path) : cb.asc(path));
    }
}
```

- [ ] **Step 6: 테스트 실행 — 성공 확인**

Run: `cd backend && ./gradlew test --tests '*StockMetricRepositoryTest'`
Expected: All 4 tests PASS

- [ ] **Step 7: 커밋**

```bash
git add backend/src/
git commit -m "feat: add StockMetric entity with Criteria API screening filters"
```

---

## Task 3: SectorMetric + IndustryMetric Entities + Repositories

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/entity/SectorMetric.java`
- Create: `backend/src/main/java/com/stockanalyzer/entity/IndustryMetric.java`
- Create: `backend/src/main/java/com/stockanalyzer/repository/SectorMetricRepository.java`
- Create: `backend/src/main/java/com/stockanalyzer/repository/IndustryMetricRepository.java`

- [ ] **Step 1: SectorMetric Entity 작성**

```java
package com.stockanalyzer.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sector_metrics",
       uniqueConstraints = @UniqueConstraint(columnNames = {"sector_id", "date"}),
       indexes = @Index(columnList = "sector_id, date"))
public class SectorMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "avg_per", precision = 10, scale = 4)
    private BigDecimal avgPer;

    @Column(name = "avg_pbr", precision = 10, scale = 4)
    private BigDecimal avgPbr;

    @Column(name = "total_market_cap", precision = 20, scale = 2)
    private BigDecimal totalMarketCap;

    @Column(name = "avg_dividend_yield", precision = 10, scale = 4)
    private BigDecimal avgDividendYield;

    @Column(name = "top_gainers", columnDefinition = "TEXT")
    private String topGainers;

    @Column(name = "top_losers", columnDefinition = "TEXT")
    private String topLosers;

    public SectorMetric() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Sector getSector() { return sector; }
    public void setSector(Sector sector) { this.sector = sector; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getAvgPer() { return avgPer; }
    public void setAvgPer(BigDecimal avgPer) { this.avgPer = avgPer; }
    public BigDecimal getAvgPbr() { return avgPbr; }
    public void setAvgPbr(BigDecimal avgPbr) { this.avgPbr = avgPbr; }
    public BigDecimal getTotalMarketCap() { return totalMarketCap; }
    public void setTotalMarketCap(BigDecimal totalMarketCap) { this.totalMarketCap = totalMarketCap; }
    public BigDecimal getAvgDividendYield() { return avgDividendYield; }
    public void setAvgDividendYield(BigDecimal avgDividendYield) { this.avgDividendYield = avgDividendYield; }
    public String getTopGainers() { return topGainers; }
    public void setTopGainers(String topGainers) { this.topGainers = topGainers; }
    public String getTopLosers() { return topLosers; }
    public void setTopLosers(String topLosers) { this.topLosers = topLosers; }
}
```

- [ ] **Step 2: IndustryMetric Entity 작성**

```java
package com.stockanalyzer.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "industry_metrics",
       uniqueConstraints = @UniqueConstraint(columnNames = {"industry_id", "date"}),
       indexes = @Index(columnList = "industry_id, date"))
public class IndustryMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_id", nullable = false)
    private Industry industry;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "avg_per", precision = 10, scale = 4)
    private BigDecimal avgPer;

    @Column(name = "avg_pbr", precision = 10, scale = 4)
    private BigDecimal avgPbr;

    @Column(name = "total_market_cap", precision = 20, scale = 2)
    private BigDecimal totalMarketCap;

    @Column(name = "stock_count")
    private Integer stockCount;

    @Column(name = "performance_1d", precision = 10, scale = 4)
    private BigDecimal performance1d;

    @Column(name = "performance_1w", precision = 10, scale = 4)
    private BigDecimal performance1w;

    @Column(name = "performance_1m", precision = 10, scale = 4)
    private BigDecimal performance1m;

    @Column(name = "performance_3m", precision = 10, scale = 4)
    private BigDecimal performance3m;

    @Column(name = "performance_1y", precision = 10, scale = 4)
    private BigDecimal performance1y;

    public IndustryMetric() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Industry getIndustry() { return industry; }
    public void setIndustry(Industry industry) { this.industry = industry; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getAvgPer() { return avgPer; }
    public void setAvgPer(BigDecimal avgPer) { this.avgPer = avgPer; }
    public BigDecimal getAvgPbr() { return avgPbr; }
    public void setAvgPbr(BigDecimal avgPbr) { this.avgPbr = avgPbr; }
    public BigDecimal getTotalMarketCap() { return totalMarketCap; }
    public void setTotalMarketCap(BigDecimal totalMarketCap) { this.totalMarketCap = totalMarketCap; }
    public Integer getStockCount() { return stockCount; }
    public void setStockCount(Integer stockCount) { this.stockCount = stockCount; }
    public BigDecimal getPerformance1d() { return performance1d; }
    public void setPerformance1d(BigDecimal performance1d) { this.performance1d = performance1d; }
    public BigDecimal getPerformance1w() { return performance1w; }
    public void setPerformance1w(BigDecimal performance1w) { this.performance1w = performance1w; }
    public BigDecimal getPerformance1m() { return performance1m; }
    public void setPerformance1m(BigDecimal performance1m) { this.performance1m = performance1m; }
    public BigDecimal getPerformance3m() { return performance3m; }
    public void setPerformance3m(BigDecimal performance3m) { this.performance3m = performance3m; }
    public BigDecimal getPerformance1y() { return performance1y; }
    public void setPerformance1y(BigDecimal performance1y) { this.performance1y = performance1y; }
}
```

- [ ] **Step 3: SectorMetricRepository 작성**

```java
package com.stockanalyzer.repository;

import com.stockanalyzer.entity.SectorMetric;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class SectorMetricRepository {

    @PersistenceContext
    private EntityManager em;

    public List<SectorMetric> findLatestAll() {
        LocalDate latestDate = getLatestDate();
        if (latestDate == null) return List.of();
        return em.createQuery(
                "SELECT sm FROM SectorMetric sm JOIN FETCH sm.sector WHERE sm.date = :date ORDER BY sm.totalMarketCap DESC",
                SectorMetric.class)
                .setParameter("date", latestDate)
                .getResultList();
    }

    public SectorMetric findLatestBySector(Long sectorId) {
        LocalDate latestDate = getLatestDate();
        if (latestDate == null) return null;
        List<SectorMetric> results = em.createQuery(
                "SELECT sm FROM SectorMetric sm WHERE sm.sector.id = :sectorId AND sm.date = :date",
                SectorMetric.class)
                .setParameter("sectorId", sectorId)
                .setParameter("date", latestDate)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public List<SectorMetric> findBySectorAndDateRange(Long sectorId, LocalDate from, LocalDate to) {
        return em.createQuery(
                "SELECT sm FROM SectorMetric sm WHERE sm.sector.id = :sectorId AND sm.date >= :from AND sm.date <= :to ORDER BY sm.date ASC",
                SectorMetric.class)
                .setParameter("sectorId", sectorId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    private LocalDate getLatestDate() {
        List<LocalDate> results = em.createQuery("SELECT MAX(sm.date) FROM SectorMetric sm", LocalDate.class).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Transactional
    public SectorMetric save(SectorMetric metric) {
        if (metric.getId() == null) { em.persist(metric); return metric; }
        return em.merge(metric);
    }
}
```

- [ ] **Step 4: IndustryMetricRepository 작성**

```java
package com.stockanalyzer.repository;

import com.stockanalyzer.entity.IndustryMetric;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class IndustryMetricRepository {

    @PersistenceContext
    private EntityManager em;

    public List<IndustryMetric> findLatestBySector(Long sectorId) {
        LocalDate latestDate = getLatestDate();
        if (latestDate == null) return List.of();
        return em.createQuery(
                "SELECT im FROM IndustryMetric im JOIN FETCH im.industry WHERE im.industry.sector.id = :sectorId AND im.date = :date",
                IndustryMetric.class)
                .setParameter("sectorId", sectorId)
                .setParameter("date", latestDate)
                .getResultList();
    }

    public IndustryMetric findLatestByIndustry(Long industryId) {
        LocalDate latestDate = getLatestDate();
        if (latestDate == null) return null;
        List<IndustryMetric> results = em.createQuery(
                "SELECT im FROM IndustryMetric im WHERE im.industry.id = :industryId AND im.date = :date",
                IndustryMetric.class)
                .setParameter("industryId", industryId)
                .setParameter("date", latestDate)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private LocalDate getLatestDate() {
        List<LocalDate> results = em.createQuery("SELECT MAX(im.date) FROM IndustryMetric im", LocalDate.class).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Transactional
    public IndustryMetric save(IndustryMetric metric) {
        if (metric.getId() == null) { em.persist(metric); return metric; }
        return em.merge(metric);
    }
}
```

- [ ] **Step 5: 빌드 확인**

Run: `cd backend && ./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add backend/src/
git commit -m "feat: add SectorMetric, IndustryMetric entities and repositories"
```

---

## Task 4: FinancialService + DTO + Tests

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/dto/FinancialData.java`
- Create: `backend/src/main/java/com/stockanalyzer/dto/FinancialRatios.java`
- Create: `backend/src/main/java/com/stockanalyzer/service/FinancialService.java`
- Test: `backend/src/test/java/com/stockanalyzer/service/FinancialServiceTest.java`

- [ ] **Step 1: DTOs 작성**

```java
package com.stockanalyzer.dto;

import java.math.BigDecimal;

public class FinancialData {
    private String period;
    private String type;
    private BigDecimal revenue;
    private BigDecimal operatingIncome;
    private BigDecimal netIncome;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    private BigDecimal operatingCashFlow;

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    public BigDecimal getOperatingIncome() { return operatingIncome; }
    public void setOperatingIncome(BigDecimal operatingIncome) { this.operatingIncome = operatingIncome; }
    public BigDecimal getNetIncome() { return netIncome; }
    public void setNetIncome(BigDecimal netIncome) { this.netIncome = netIncome; }
    public BigDecimal getTotalAssets() { return totalAssets; }
    public void setTotalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; }
    public BigDecimal getTotalLiabilities() { return totalLiabilities; }
    public void setTotalLiabilities(BigDecimal totalLiabilities) { this.totalLiabilities = totalLiabilities; }
    public BigDecimal getTotalEquity() { return totalEquity; }
    public void setTotalEquity(BigDecimal totalEquity) { this.totalEquity = totalEquity; }
    public BigDecimal getOperatingCashFlow() { return operatingCashFlow; }
    public void setOperatingCashFlow(BigDecimal operatingCashFlow) { this.operatingCashFlow = operatingCashFlow; }
}
```

```java
package com.stockanalyzer.dto;

import java.math.BigDecimal;
import java.util.Map;

public class FinancialRatios {
    private Map<String, BigDecimal> stock;
    private Map<String, BigDecimal> sectorAverage;

    public FinancialRatios(Map<String, BigDecimal> stock, Map<String, BigDecimal> sectorAverage) {
        this.stock = stock;
        this.sectorAverage = sectorAverage;
    }

    public Map<String, BigDecimal> getStock() { return stock; }
    public Map<String, BigDecimal> getSectorAverage() { return sectorAverage; }
}
```

- [ ] **Step 2: 테스트 작성**

```java
package com.stockanalyzer.service;

import com.stockanalyzer.dto.FinancialData;
import com.stockanalyzer.dto.FinancialRatios;
import com.stockanalyzer.entity.Financial;
import com.stockanalyzer.entity.Industry;
import com.stockanalyzer.entity.Sector;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.repository.FinancialRepository;
import com.stockanalyzer.repository.StockMetricRepository;
import com.stockanalyzer.repository.StockRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FinancialServiceTest {

    @Mock private FinancialRepository financialRepository;
    @Mock private StockRepository stockRepository;
    @Mock private StockMetricRepository stockMetricRepository;
    @InjectMocks private FinancialService financialService;

    private Stock apple;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Sector tech = new Sector("Technology", "Tech");
        tech.setId(1L);
        Industry sw = new Industry("Software", "SW", tech);
        sw.setId(1L);
        apple = new Stock();
        apple.setId(1L);
        apple.setTicker("AAPL");
        apple.setCompanyName("Apple Inc.");
        apple.setIndustry(sw);
    }

    @Test
    public void getFinancials_returnsConvertedDTOs() {
        Financial f = new Financial();
        f.setStock(apple);
        f.setPeriod("2025");
        f.setType("annual");
        f.setRevenue(new BigDecimal("394328000000"));
        f.setNetIncome(new BigDecimal("96995000000"));
        f.setOperatingIncome(new BigDecimal("114301000000"));
        f.setTotalAssets(new BigDecimal("352583000000"));
        f.setTotalLiabilities(new BigDecimal("290437000000"));
        f.setTotalEquity(new BigDecimal("62146000000"));
        f.setOperatingCashFlow(new BigDecimal("110543000000"));

        when(stockRepository.findByTicker("AAPL")).thenReturn(apple);
        when(financialRepository.findByStockAndType(1L, "annual")).thenReturn(Arrays.asList(f));

        List<FinancialData> results = financialService.getFinancials("AAPL", "annual");
        assertEquals(1, results.size());
        assertEquals("2025", results.get(0).getPeriod());
        assertEquals(new BigDecimal("394328000000"), results.get(0).getRevenue());
    }

    @Test
    public void getRatios_calculatesCorrectly() {
        Financial f = new Financial();
        f.setStock(apple);
        f.setPeriod("2025");
        f.setType("annual");
        f.setRevenue(new BigDecimal("394328000000"));
        f.setOperatingIncome(new BigDecimal("114301000000"));
        f.setNetIncome(new BigDecimal("96995000000"));
        f.setTotalAssets(new BigDecimal("352583000000"));
        f.setTotalLiabilities(new BigDecimal("290437000000"));
        f.setTotalEquity(new BigDecimal("62146000000"));
        f.setOperatingCashFlow(new BigDecimal("110543000000"));

        when(stockRepository.findByTicker("AAPL")).thenReturn(apple);
        when(financialRepository.findLatestByStock(1L, "annual")).thenReturn(f);

        FinancialRatios ratios = financialService.getRatios("AAPL");
        assertNotNull(ratios.getStock());
        assertTrue(ratios.getStock().get("roe").compareTo(BigDecimal.ZERO) > 0);
        assertTrue(ratios.getStock().get("operatingMargin").compareTo(BigDecimal.ZERO) > 0);
    }
}
```

- [ ] **Step 3: FinancialService 작성**

```java
package com.stockanalyzer.service;

import com.stockanalyzer.dto.FinancialData;
import com.stockanalyzer.dto.FinancialRatios;
import com.stockanalyzer.entity.Financial;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.repository.FinancialRepository;
import com.stockanalyzer.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinancialService {

    private final FinancialRepository financialRepository;
    private final StockRepository stockRepository;

    @Autowired
    public FinancialService(FinancialRepository financialRepository, StockRepository stockRepository) {
        this.financialRepository = financialRepository;
        this.stockRepository = stockRepository;
    }

    public List<FinancialData> getFinancials(String ticker, String type) {
        Stock stock = stockRepository.findByTicker(ticker.toUpperCase());
        if (stock == null) return List.of();
        return financialRepository.findByStockAndType(stock.getId(), type)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public FinancialRatios getRatios(String ticker) {
        Stock stock = stockRepository.findByTicker(ticker.toUpperCase());
        if (stock == null) return new FinancialRatios(Map.of(), Map.of());

        Financial latest = financialRepository.findLatestByStock(stock.getId(), "annual");
        if (latest == null) return new FinancialRatios(Map.of(), Map.of());

        Map<String, BigDecimal> stockRatios = calculateRatios(latest);

        // 섹터 평균은 Phase 2 MetricsCalculator가 채운 후 사용 가능
        // 현재는 빈 맵 반환 (MetricsCalculator와 연동 시 채워짐)
        Map<String, BigDecimal> sectorAvg = new HashMap<>();

        return new FinancialRatios(stockRatios, sectorAvg);
    }

    private Map<String, BigDecimal> calculateRatios(Financial f) {
        Map<String, BigDecimal> ratios = new HashMap<>();
        BigDecimal equity = f.getTotalEquity();
        BigDecimal assets = f.getTotalAssets();
        BigDecimal revenue = f.getRevenue();
        BigDecimal liabilities = f.getTotalLiabilities();

        if (equity != null && equity.compareTo(BigDecimal.ZERO) != 0) {
            ratios.put("roe", f.getNetIncome().divide(equity, 4, RoundingMode.HALF_UP));
            ratios.put("debtRatio", liabilities.divide(equity, 4, RoundingMode.HALF_UP));
        }
        if (assets != null && assets.compareTo(BigDecimal.ZERO) != 0) {
            ratios.put("roa", f.getNetIncome().divide(assets, 4, RoundingMode.HALF_UP));
        }
        if (revenue != null && revenue.compareTo(BigDecimal.ZERO) != 0) {
            ratios.put("operatingMargin", f.getOperatingIncome().divide(revenue, 4, RoundingMode.HALF_UP));
            ratios.put("netMargin", f.getNetIncome().divide(revenue, 4, RoundingMode.HALF_UP));
        }

        return ratios;
    }

    private FinancialData toDto(Financial f) {
        FinancialData d = new FinancialData();
        d.setPeriod(f.getPeriod());
        d.setType(f.getType());
        d.setRevenue(f.getRevenue());
        d.setOperatingIncome(f.getOperatingIncome());
        d.setNetIncome(f.getNetIncome());
        d.setTotalAssets(f.getTotalAssets());
        d.setTotalLiabilities(f.getTotalLiabilities());
        d.setTotalEquity(f.getTotalEquity());
        d.setOperatingCashFlow(f.getOperatingCashFlow());
        return d;
    }
}
```

- [ ] **Step 4: 테스트 실행 — 성공 확인**

Run: `cd backend && ./gradlew test --tests '*FinancialServiceTest'`
Expected: All 2 tests PASS

- [ ] **Step 5: 커밋**

```bash
git add backend/src/
git commit -m "feat: add FinancialService with ratios calculation and tests"
```

---

## Task 5: ScreeningService + SectorService + DTOs

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/dto/ScreeningResult.java`
- Create: `backend/src/main/java/com/stockanalyzer/dto/SectorOverview.java`
- Create: `backend/src/main/java/com/stockanalyzer/dto/SectorDetail.java`
- Create: `backend/src/main/java/com/stockanalyzer/dto/SectorPerformance.java`
- Create: `backend/src/main/java/com/stockanalyzer/dto/IndustryDetail.java`
- Create: `backend/src/main/java/com/stockanalyzer/service/ScreeningService.java`
- Create: `backend/src/main/java/com/stockanalyzer/service/SectorService.java`
- Test: `backend/src/test/java/com/stockanalyzer/service/ScreeningServiceTest.java`

이 Task는 코드량이 많으므로 구현 subagent에게 전체 코드를 제공한다. DTOs는 단순 POJO이고, Service는 Repository를 위임하는 얇은 레이어다.

ScreeningResult 필드: ticker, companyName, exchange, marketCap, per, pbr, roe, debtRatio, dividendYield, revenueGrowth, operatingMargin
SectorOverview 필드: sectorId, sectorName, avgPer, avgPbr, totalMarketCap, avgDividendYield
SectorDetail 필드: sectorId, sectorName, description, metric(SectorOverview), industries(List of IndustryDetail)
SectorPerformance 필드: sectorId, sectorName, performances(Map<String, BigDecimal> — 기간별 수익률)
IndustryDetail 필드: industryId, industryName, avgPer, avgPbr, totalMarketCap, stockCount, performances

- [ ] **Step 1: 모든 DTO 작성** (각각 개별 파일)
- [ ] **Step 2: ScreeningService 작성** — StockMetricRepository.findByFilters 위임 + toDto 변환
- [ ] **Step 3: SectorService 작성** — SectorMetricRepository, IndustryMetricRepository 위임
- [ ] **Step 4: ScreeningServiceTest 작성 및 실행**
- [ ] **Step 5: 전체 테스트 확인 및 커밋**

```bash
git add backend/src/
git commit -m "feat: add ScreeningService, SectorService with DTOs"
```

---

## Task 6: Controllers (Screening + Sector + StockController 수정)

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/controller/ScreeningController.java`
- Create: `backend/src/main/java/com/stockanalyzer/controller/SectorController.java`
- Modify: `backend/src/main/java/com/stockanalyzer/controller/StockController.java`
- Test: `backend/src/test/java/com/stockanalyzer/controller/ScreeningControllerTest.java`
- Test: `backend/src/test/java/com/stockanalyzer/controller/SectorControllerTest.java`

- [ ] **Step 1: ScreeningController 작성**

```java
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
            @RequestParam(required = false) Long sector,
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
            @RequestParam(defaultValue = "marketCap,desc") String sort) {

        ScreeningFilter filter = new ScreeningFilter();
        filter.setSectorId(sector);
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

        Map<String, Object> meta = new HashMap<>();
        meta.put("page", page);
        meta.put("size", size);
        meta.put("totalCount", totalCount);
        meta.put("totalPages", (int) Math.ceil((double) totalCount / size));

        return ApiResponse.ok(results, meta);
    }
}
```

- [ ] **Step 2: SectorController 작성**

```java
package com.stockanalyzer.controller;

import com.stockanalyzer.common.ApiResponse;
import com.stockanalyzer.dto.IndustryDetail;
import com.stockanalyzer.dto.SectorDetail;
import com.stockanalyzer.dto.SectorOverview;
import com.stockanalyzer.service.SectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SectorController {

    private final SectorService sectorService;

    @Autowired
    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @GetMapping("/sectors")
    public ApiResponse<List<SectorOverview>> getAllSectors() {
        return ApiResponse.ok(sectorService.getAllSectors());
    }

    @GetMapping("/sectors/{id}")
    public ApiResponse<SectorDetail> getSectorDetail(@PathVariable Long id) {
        return ApiResponse.ok(sectorService.getSectorDetail(id));
    }

    @GetMapping("/sectors/{id}/rankings")
    public ApiResponse<List<?>> getSectorRankings(
            @PathVariable Long id,
            @RequestParam(defaultValue = "gainers") String sort) {
        return ApiResponse.ok(sectorService.getSectorRankings(id, sort));
    }

    @GetMapping("/sectors/compare")
    public ApiResponse<List<SectorOverview>> compareSectors() {
        return ApiResponse.ok(sectorService.getAllSectors());
    }

    @GetMapping("/industries/{id}")
    public ApiResponse<IndustryDetail> getIndustryDetail(@PathVariable Long id) {
        return ApiResponse.ok(sectorService.getIndustryDetail(id));
    }
}
```

- [ ] **Step 3: StockController에 financials 엔드포인트 추가**

StockController.java에 FinancialService 주입 및 2개 엔드포인트 추가:

```java
@GetMapping("/{ticker}/financials")
public ApiResponse<List<FinancialData>> getFinancials(
        @PathVariable String ticker,
        @RequestParam(defaultValue = "annual") String type) {
    return ApiResponse.ok(financialService.getFinancials(ticker, type));
}

@GetMapping("/{ticker}/financials/ratios")
public ApiResponse<FinancialRatios> getRatios(@PathVariable String ticker) {
    return ApiResponse.ok(financialService.getRatios(ticker));
}
```

- [ ] **Step 4: 컨트롤러 테스트 작성 및 실행**
- [ ] **Step 5: 전체 테스트 확인 및 커밋**

```bash
git add backend/src/
git commit -m "feat: add ScreeningController, SectorController, and financial endpoints"
```

---

## Task 7: FinancialCollector + MetricsCalculator Schedulers

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/scheduler/FinancialCollector.java`
- Create: `backend/src/main/java/com/stockanalyzer/scheduler/MetricsCalculator.java`

- [ ] **Step 1: FinancialCollector 작성**

Yahoo Finance의 financials 데이터를 파싱하여 Financial 엔티티에 저장. `@Scheduled(cron = "0 0 7 ? * SUN")`.

- [ ] **Step 2: MetricsCalculator 작성**

`@Scheduled(cron = "0 30 7 ? * TUE-SAT")`. 모든 종목의 stock_metrics 계산 → sector_metrics 집계 → industry_metrics 집계.

- [ ] **Step 3: 빌드 확인 및 커밋**

```bash
git add backend/src/
git commit -m "feat: add FinancialCollector and MetricsCalculator schedulers"
```

---

## Task 8: Frontend API + Screening Page

**Files:**
- Modify: `frontend/src/api/stockApi.ts`
- Create: `frontend/src/components/FilterPanel.tsx`
- Create: `frontend/src/pages/Screening.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: stockApi.ts에 새 인터페이스 및 API 함수 추가**

추가할 인터페이스: ScreeningResult, ScreeningFilter, FinancialData, FinancialRatios, SectorOverview, SectorDetail, IndustryDetail
추가할 함수: screenStocks(), getFinancials(), getFinancialRatios(), getSectors(), getSectorDetail(), getSectorRankings(), getIndustryDetail(), compareSectors()

- [ ] **Step 2: FilterPanel.tsx 작성** — 12가지 필터 입력 (범위 input, 섹터 select, 거래소 select)
- [ ] **Step 3: Screening.tsx 작성** — FilterPanel + 결과 테이블 + 페이징
- [ ] **Step 4: App.tsx에 라우트 추가**
- [ ] **Step 5: 빌드 확인 및 커밋**

```bash
git add frontend/src/
git commit -m "feat: add screening page with 12-filter panel"
```

---

## Task 9: Frontend Financial Tabs on StockDetail

**Files:**
- Create: `frontend/src/components/FinancialTable.tsx`
- Create: `frontend/src/components/RatioComparison.tsx`
- Modify: `frontend/src/pages/StockDetail.tsx`

- [ ] **Step 1: FinancialTable.tsx 작성** — 탭(요약/손익/대차/현금흐름) + 연도별 테이블
- [ ] **Step 2: RatioComparison.tsx 작성** — Recharts BarChart로 종목 vs 섹터 평균 비교
- [ ] **Step 3: StockDetail.tsx 수정** — 차트 아래에 재무제표 탭 영역 추가
- [ ] **Step 4: 빌드 확인 및 커밋**

```bash
git add frontend/src/
git commit -m "feat: add financial tables and ratio comparison to stock detail"
```

---

## Task 10: Frontend Sector Analysis Pages

**Files:**
- Create: `frontend/src/components/SectorHeatmap.tsx`
- Create: `frontend/src/components/PerformanceChart.tsx`
- Create: `frontend/src/pages/SectorAnalysis.tsx`
- Create: `frontend/src/pages/IndustryDetail.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: SectorHeatmap.tsx 작성** — Recharts Treemap (시가총액 크기, 퍼포먼스 색상)
- [ ] **Step 2: PerformanceChart.tsx 작성** — Recharts BarChart (섹터별 기간 수익률)
- [ ] **Step 3: SectorAnalysis.tsx 작성** — 상단 히트맵+차트, 하단 테이블, 섹터 클릭 → 드릴다운
- [ ] **Step 4: IndustryDetail.tsx 작성** — 산업 상세 + 종목 리스트
- [ ] **Step 5: App.tsx 라우트 추가** (`/sectors`, `/sectors/:id`, `/industries/:id`)
- [ ] **Step 6: 빌드 확인 및 커밋**

```bash
git add frontend/src/
git commit -m "feat: add sector analysis pages with heatmap and drilldown"
```

---

## Task 11: Integration Test

**Files:** 없음 (기존 파일로 테스트)

- [ ] **Step 1: 백엔드 전체 테스트**

Run: `cd backend && ./gradlew test`
Expected: All tests PASS

- [ ] **Step 2: 프론트엔드 빌드**

Run: `cd frontend && npm run build`
Expected: 빌드 성공

- [ ] **Step 3: 최종 커밋**

```bash
git add -A
git commit -m "chore: Phase 2 complete — screening, financials, sector analysis"
```
