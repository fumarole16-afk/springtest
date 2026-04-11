# Data Pipeline Fix Design

## Problem

4개 스케줄러(PriceCollector, MetricsCalculator, FinancialCollector, NewsCollector)가 데이터를 제대로 수집하지 못하고 있음.

- `marketCap`: 모든 종목 `0.00` (Yahoo v8 chart API에 marketCap 없음, 하드코딩 ZERO)
- PER/PBR/ROE 등: 모든 종목 `null` (Yahoo v10 quoteSummary API 인증 실패 -> 재무 데이터 없음)
- 뉴스: 없음 (Finnhub API 키 미설정)

## Approach

Yahoo Finance v8 유지 (가격) + Yahoo v6 quote API (marketCap) + SEC EDGAR XBRL API (재무) + SEC EDGAR Atom (뉴스)

외부 API 키 불필요. SEC EDGAR는 User-Agent 헤더만 필요.

## Changes

### 1. marketCap: Yahoo v6 quote API

**파일:** `YahooFinanceClient.java`

- `fetchMarketCaps(List<String> tickers)` 메서드 추가
- 엔드포인트: `GET /v6/finance/quote?symbols=AAPL,MSFT,...`
- 응답에서 `marketCap` 필드 추출
- 배치 조회 가능 (쉼표 구분)

**파일:** `PriceCollector.java`

- `collectDailyPrices()` 마지막에 `fetchMarketCaps()` 호출
- 기존/신규 종목 모두 marketCap 갱신
- `collectForTicker()`에서도 기존 종목일 때 marketCap 업데이트

### 2. 재무제표: SEC EDGAR XBRL API

**파일:** `SecEdgarClient.java`

- ticker -> CIK 매핑 메서드 추가
  - 소스: `https://www.sec.gov/files/company_tickers.json`
  - 서버 시작 시 또는 최초 호출 시 캐싱
- `fetchFinancialFacts(String ticker)` 메서드 추가
  - 엔드포인트: `https://data.sec.gov/api/xbrl/companyfacts/CIK{cik}.json`
  - User-Agent 헤더 필수 (SEC 정책)
  - rate limit: 10 req/sec

XBRL 택소노미 매핑:

| Financial 필드 | XBRL 컨셉 |
|---------------|-----------|
| revenue | `us-gaap:Revenues` 또는 `us-gaap:RevenueFromContractWithCustomerExcludingAssessedTax` |
| netIncome | `us-gaap:NetIncomeLoss` |
| operatingIncome | `us-gaap:OperatingIncomeLoss` |
| totalAssets | `us-gaap:Assets` |
| totalLiabilities | `us-gaap:Liabilities` |
| totalEquity | `us-gaap:StockholdersEquity` |
| operatingCashFlow | `us-gaap:NetCashProvidedByOperatingActivities` |

annual 데이터만 수집 (form: 10-K). period는 fiscal year end date에서 연도 추출.

**파일:** `FinancialCollector.java`

- Yahoo v10 호출을 SEC EDGAR XBRL 호출로 교체
- `SecEdgarClient.fetchFinancialFacts()` 사용
- 파싱 로직을 XBRL JSON 구조에 맞게 변경

### 3. 뉴스: SEC EDGAR Atom 피드

**파일:** `NewsCollector.java`

- Finnhub 호출을 `SecEdgarClient.fetchFilings()` 호출로 교체
- 기존 `SecEdgarClient.fetchFilings()` 메서드 그대로 활용
- `FinnhubClient` 의존성 제거

### 4. 수동 트리거 엔드포인트

**파일:** `AdminController.java`

추가 엔드포인트:
- `GET /api/admin/collect-prices` -> PriceCollector.collectDailyPrices()
- `GET /api/admin/collect-financials` -> FinancialCollector.collectFinancials()
- `GET /api/admin/calculate-metrics` -> MetricsCalculator.run()
- `GET /api/admin/collect-news` -> NewsCollector 수동 실행

## Scope

- 미국 상장 종목만 지원 (SEC EDGAR 한계)
- 현재 40개 종목 전부 미국 종목이므로 문제 없음
- dividendYield 계산은 이번 범위에 포함하지 않음 (배당 데이터 소스 별도 필요)

## Files to Modify

| 파일 | 변경 내용 |
|------|----------|
| `YahooFinanceClient.java` | v6 quote API로 marketCap 배치 조회 추가 |
| `PriceCollector.java` | 기존 종목 marketCap 갱신 로직 추가 |
| `SecEdgarClient.java` | ticker->CIK 매핑, XBRL 재무 데이터 조회 추가 |
| `FinancialCollector.java` | Yahoo -> SEC EDGAR로 교체 |
| `NewsCollector.java` | Finnhub -> SEC EDGAR filings로 교체 |
| `AdminController.java` | 수동 트리거 엔드포인트 추가 |
