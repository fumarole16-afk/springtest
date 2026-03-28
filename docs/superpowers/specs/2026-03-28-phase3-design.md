# Phase 3: News/Filings + Stock Comparison + Dashboard — Design Spec

## Overview

Phase 1-2 위에 뉴스/SEC 공시, 종목 비교(고급), 대시보드 고도화를 추가하여 전체 기능을 완성한다.

## Data Model

### 새로 추가하는 테이블

| 테이블 | 주요 컬럼 |
|--------|-----------|
| `news` | id, title, source, url, published_at, summary, image_url |
| `stock_news` | stock_id, news_id (N:M 조인 테이블) |

### 관계

- `stocks` N:M `news` (via `stock_news`)

## Data Sources

- **Finnhub** — 종목별 뉴스 (`/company-news?symbol={ticker}&from=&to=`). 무료 60호출/분
- **SEC EDGAR** — 공시 (`/cgi-bin/browse-edgar` Atom feed). 무료, 제한 없음
- **Yahoo Finance** — 주요 지수 quote (^GSPC, ^IXIC, ^DJI)

## API Design

### 뉴스/공시

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/stocks/{ticker}/news?page=&size=` | 종목별 뉴스 |
| GET | `/api/news?page=&size=` | 전체 최신 뉴스 |
| GET | `/api/news/filings?ticker={ticker}` | SEC 공시 |

### 종목 비교

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/compare?tickers=AAPL,MSFT,GOOGL` | 비교 데이터 (지표 + 가격 이력 + 재무제표) |

응답 구조:
- `stocks`: 각 종목의 기본 지표 (ticker, companyName, currentPrice, marketCap, per, pbr, roe, operatingMargin, debtRatio, dividendYield, revenueGrowth)
- `prices`: 종목별 가격 이력 (Map<ticker, List<PriceData>>)
- `financials`: 종목별 재무제표 (Map<ticker, List<FinancialData>>)

### 대시보드

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/dashboard/indices` | 주요 지수 현재가 (S&P500, NASDAQ, DOW) |
| GET | `/api/dashboard/movers` | 상승/하락 Top 5 종목 |
| GET | `/api/dashboard/extremes` | 52주 신고가/신저가 종목 |
| GET | `/api/dashboard/volume-spikes` | 거래량 급등 종목 (30일 평균 대비 2배 이상) |

## Backend

### 새로 추가하는 클래스

```
com.stockanalyzer
├── entity/
│   ├── News.java
│   └── StockNews.java
├── repository/
│   └── NewsRepository.java
├── dto/
│   ├── NewsItem.java
│   ├── CompareData.java
│   ├── IndexQuote.java
│   └── MoverStock.java
├── service/
│   ├── NewsService.java
│   ├── CompareService.java
│   └── DashboardService.java
├── controller/
│   ├── NewsController.java
│   ├── CompareController.java
│   └── DashboardController.java
├── client/
│   ├── FinnhubClient.java
│   └── SecEdgarClient.java
└── scheduler/
    └── NewsCollector.java
```

### 기존 파일 수정

- `StockController.java`: `/api/stocks/{ticker}/news` 엔드포인트 추가

### 스케줄러

| 스케줄러 | 크론 | 동작 |
|---------|------|------|
| `NewsCollector` | `0 0 */6 * * *` (매 6시간) | Finnhub에서 종목별 뉴스 수집, SEC EDGAR에서 공시 수집 → news + stock_news 저장 |

## Frontend

### 새로 추가하는 파일

| 파일 | 역할 |
|------|------|
| `pages/Compare.tsx` | 티커 입력(태그) + 수익률 차트 + 지표 테이블 + 레이더 차트 + 재무 비교 + 트렌드 |
| `pages/News.tsx` | 전체/종목별 뉴스 피드 + SEC 공시 탭 |
| `components/CompareChart.tsx` | 수익률 오버레이 라인 차트 (Recharts LineChart, 기준일 대비 % 변환) |
| `components/RadarComparison.tsx` | 재무비율 레이더 차트 (Recharts RadarChart) |
| `components/TickerInput.tsx` | 태그 형태 멀티 티커 입력 (자동완성 + 태그 추가/삭제) |
| `components/NewsFeed.tsx` | 뉴스 카드 리스트 (이미지 + 제목 + 요약 + 날짜) |
| `components/IndexCards.tsx` | 주요 지수 카드 3개 (현재가 + 등락률) |
| `components/MoverTable.tsx` | 상승/하락 Top 5 테이블 |

### 기존 파일 수정

| 파일 | 변경 |
|------|------|
| `App.tsx` | `/compare`, `/news` 라우트 추가 |
| `pages/Dashboard.tsx` | 플레이스홀더 → 풀 대시보드 (IndexCards + SectorHeatmap + MoverTable + 신고가/신저가 + 거래량 급등 + NewsFeed) |
| `pages/StockDetail.tsx` | 뉴스 탭 추가 (NewsFeed 컴포넌트 재사용) |
| `api/stockApi.ts` | 뉴스, 비교, 대시보드 API 함수 추가 |

### 대시보드 레이아웃

```
┌─────────────────────────────────────────┐
│  [S&P 500]    [NASDAQ]    [DOW]         │  ← IndexCards
├─────────────────────────────────────────┤
│  섹터 히트맵 (SectorHeatmap 재사용)       │
├──────────────────┬──────────────────────┤
│  상승 Top 5      │  하락 Top 5          │  ← MoverTable x2
├──────────────────┴──────────────────────┤
│  52주 신고가/신저가  │  거래량 급등        │
├─────────────────────────────────────────┤
│  최근 뉴스 5건 (NewsFeed)               │
└─────────────────────────────────────────┘
```
