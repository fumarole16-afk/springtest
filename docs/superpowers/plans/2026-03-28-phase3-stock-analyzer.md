# Phase 3: News/Filings + Comparison + Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 뉴스/SEC 공시, 종목 비교(고급), 대시보드 고도화를 추가하여 전체 플랫폼을 완성한다.

**Architecture:** Finnhub과 SEC EDGAR에서 뉴스/공시를 수집하는 NewsCollector 스케줄러, 종목 비교를 위한 CompareService, 대시보드 데이터를 위한 DashboardService를 추가한다. 프론트엔드는 Recharts RadarChart로 비교 레이더, 풀 대시보드 레이아웃을 구현한다.

**Tech Stack:** 기존 Phase 1-2 스택 + Finnhub API, SEC EDGAR Atom Feed, Recharts RadarChart

---

## Task 1: News + StockNews Entities + Repository

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/entity/News.java`
- Create: `backend/src/main/java/com/stockanalyzer/entity/StockNews.java`
- Create: `backend/src/main/java/com/stockanalyzer/repository/NewsRepository.java`
- Test: `backend/src/test/java/com/stockanalyzer/repository/NewsRepositoryTest.java`

News entity: id, title, source, url, publishedAt(LocalDateTime), summary, imageUrl
StockNews entity: id, stock(ManyToOne), news(ManyToOne) — composite unique on (stock_id, news_id)
NewsRepository: findByStockTicker(ticker, page, size), findLatest(page, size), save, saveStockNews

- [ ] **Step 1: 테스트 작성**
- [ ] **Step 2: 테스트 실행 — 실패 확인**
- [ ] **Step 3: Entity + Repository 작성**
- [ ] **Step 4: 테스트 실행 — 성공 확인**
- [ ] **Step 5: 커밋** `git commit -m "feat: add News, StockNews entities and repository"`

---

## Task 2: Finnhub + SEC EDGAR Clients

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/client/FinnhubClient.java`
- Create: `backend/src/main/java/com/stockanalyzer/client/SecEdgarClient.java`
- Test: `backend/src/test/java/com/stockanalyzer/client/FinnhubClientTest.java`

FinnhubClient: fetchCompanyNews(ticker, from, to) — GET https://finnhub.io/api/v1/company-news?symbol={ticker}&from={from}&to={to}&token={apiKey}. Returns List of NewsItem DTOs.
SecEdgarClient: fetchFilings(ticker, count) — GET SEC EDGAR Atom feed, parse XML. Returns List of NewsItem DTOs.

application.properties에 추가: `finnhub.api-key=` (빈 값, 사용자가 설정)

- [ ] **Step 1: NewsItem DTO 작성** — title, source, url, publishedAt, summary, imageUrl
- [ ] **Step 2: FinnhubClient 테스트 (JSON 파싱)**
- [ ] **Step 3: FinnhubClient 작성**
- [ ] **Step 4: SecEdgarClient 작성** (XML 파싱, javax.xml.parsers 사용)
- [ ] **Step 5: 테스트 실행 — 성공 확인**
- [ ] **Step 6: 커밋** `git commit -m "feat: add Finnhub and SEC EDGAR clients"`

---

## Task 3: NewsService + NewsController + NewsCollector

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/service/NewsService.java`
- Create: `backend/src/main/java/com/stockanalyzer/controller/NewsController.java`
- Create: `backend/src/main/java/com/stockanalyzer/scheduler/NewsCollector.java`
- Modify: `backend/src/main/java/com/stockanalyzer/controller/StockController.java` — news endpoint 추가

NewsService: getNewsByTicker(ticker, page, size), getLatestNews(page, size), getFilings(ticker)
NewsController: GET /api/news, GET /api/news/filings?ticker=
StockController 추가: GET /api/stocks/{ticker}/news
NewsCollector: @Scheduled(cron = "0 0 */6 * * *"), 모든 종목 순회하며 Finnhub + SEC EDGAR 수집

- [ ] **Step 1: NewsService 작성**
- [ ] **Step 2: NewsController 작성**
- [ ] **Step 3: StockController에 news endpoint 추가**
- [ ] **Step 4: NewsCollector 스케줄러 작성**
- [ ] **Step 5: 테스트 + 빌드 확인**
- [ ] **Step 6: 커밋** `git commit -m "feat: add news service, controller, and collector scheduler"`

---

## Task 4: CompareService + CompareController

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/dto/CompareData.java`
- Create: `backend/src/main/java/com/stockanalyzer/service/CompareService.java`
- Create: `backend/src/main/java/com/stockanalyzer/controller/CompareController.java`
- Test: `backend/src/test/java/com/stockanalyzer/service/CompareServiceTest.java`

CompareData: stocks(List<StockDetail>), prices(Map<String, List<PriceData>>), financials(Map<String, List<FinancialData>>)
CompareService: compare(List<String> tickers, String period) — 각 ticker의 detail + prices + financials 조합
CompareController: GET /api/compare?tickers=AAPL,MSFT&period=1m

- [ ] **Step 1: CompareData DTO 작성**
- [ ] **Step 2: CompareService 테스트 작성**
- [ ] **Step 3: CompareService 작성** (StockService, FinancialService 위임)
- [ ] **Step 4: CompareController 작성**
- [ ] **Step 5: 테스트 실행 — 성공 확인**
- [ ] **Step 6: 커밋** `git commit -m "feat: add stock comparison service and controller"`

---

## Task 5: DashboardService + DashboardController

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/dto/IndexQuote.java`
- Create: `backend/src/main/java/com/stockanalyzer/dto/MoverStock.java`
- Create: `backend/src/main/java/com/stockanalyzer/service/DashboardService.java`
- Create: `backend/src/main/java/com/stockanalyzer/controller/DashboardController.java`

IndexQuote: symbol, name, price, change, changePercent
MoverStock: ticker, companyName, price, changePercent

DashboardService:
- getIndices() — YahooFinanceClient.fetchQuote for ^GSPC, ^IXIC, ^DJI
- getMovers() — stock_metrics에서 일간 변동률 상위/하위 5
- getExtremes() — stock_metrics에서 현재가 ≈ week52_high or week52_low
- getVolumeSpikes() — daily_prices에서 당일 거래량 / avg_volume_30d > 2

DashboardController: GET /api/dashboard/indices, /movers, /extremes, /volume-spikes

- [ ] **Step 1: DTOs 작성**
- [ ] **Step 2: DashboardService 작성**
- [ ] **Step 3: DashboardController 작성**
- [ ] **Step 4: 빌드 확인**
- [ ] **Step 5: 커밋** `git commit -m "feat: add dashboard service with indices, movers, extremes"`

---

## Task 6: Frontend API + News Page

**Files:**
- Modify: `frontend/src/api/stockApi.ts` — 뉴스, 비교, 대시보드 API 함수 추가
- Create: `frontend/src/components/NewsFeed.tsx` — 뉴스 카드 리스트
- Create: `frontend/src/pages/News.tsx` — 전체 뉴스 + 공시 탭
- Modify: `frontend/src/pages/StockDetail.tsx` — 뉴스 섹션 추가

stockApi.ts 추가: NewsItem, CompareData, IndexQuote, MoverStock 인터페이스 + getNews, getStockNews, getFilings, compareStocks, getDashboardIndices, getDashboardMovers, getDashboardExtremes, getDashboardVolumeSpikes 함수

- [ ] **Step 1: stockApi.ts에 인터페이스 + 함수 추가**
- [ ] **Step 2: NewsFeed.tsx 작성** (이미지 + 제목 + 요약 + 날짜 카드)
- [ ] **Step 3: News.tsx 페이지 작성** (전체 뉴스 탭 + SEC 공시 탭)
- [ ] **Step 4: StockDetail.tsx에 뉴스 섹션 추가**
- [ ] **Step 5: 빌드 확인 및 커밋** `git commit -m "feat: add news page and feed component"`

---

## Task 7: Frontend Compare Page

**Files:**
- Create: `frontend/src/components/TickerInput.tsx` — 태그 형태 멀티 티커 입력 + 자동완성
- Create: `frontend/src/components/CompareChart.tsx` — 수익률 오버레이 (Recharts LineChart, % 변환)
- Create: `frontend/src/components/RadarComparison.tsx` — 재무비율 레이더 (Recharts RadarChart)
- Create: `frontend/src/pages/Compare.tsx`

Compare 페이지 구성:
1. TickerInput (2~5개 태그)
2. 수익률 차트 (CompareChart)
3. 기본 지표 비교 테이블
4. 재무비율 레이더 차트 (RadarComparison)
5. 재무제표 비교 테이블
6. 배당/성장률 트렌드 (Recharts LineChart)

- [ ] **Step 1: TickerInput.tsx 작성**
- [ ] **Step 2: CompareChart.tsx 작성**
- [ ] **Step 3: RadarComparison.tsx 작성**
- [ ] **Step 4: Compare.tsx 페이지 작성**
- [ ] **Step 5: 빌드 확인 및 커밋** `git commit -m "feat: add stock comparison page with radar chart"`

---

## Task 8: Frontend Dashboard 고도화

**Files:**
- Create: `frontend/src/components/IndexCards.tsx` — 주요 지수 카드 3개
- Create: `frontend/src/components/MoverTable.tsx` — 상승/하락 Top 5 테이블
- Modify: `frontend/src/pages/Dashboard.tsx` — 풀 대시보드로 교체

Dashboard 레이아웃: IndexCards → SectorHeatmap(재사용) → MoverTable x2 → 52주 신고가/신저가 → 거래량 급등 → NewsFeed(최근 5건)

- [ ] **Step 1: IndexCards.tsx 작성**
- [ ] **Step 2: MoverTable.tsx 작성**
- [ ] **Step 3: Dashboard.tsx 풀 대시보드로 교체**
- [ ] **Step 4: 빌드 확인 및 커밋** `git commit -m "feat: enhance dashboard with indices, movers, and news"`

---

## Task 9: App.tsx 라우팅 + Integration Test

**Files:**
- Modify: `frontend/src/App.tsx` — /compare, /news 라우트 추가

- [ ] **Step 1: App.tsx 라우트 추가**
- [ ] **Step 2: 백엔드 전체 테스트** `./gradlew test`
- [ ] **Step 3: 프론트엔드 빌드** `npm run build`
- [ ] **Step 4: 최종 커밋** `git commit -m "chore: Phase 3 complete — news, comparison, dashboard"`
