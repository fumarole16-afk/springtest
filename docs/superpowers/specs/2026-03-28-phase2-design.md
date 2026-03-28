# Phase 2: Screening + Financials + Sector Analysis — Design Spec

## Overview

Phase 1의 종목 검색/차트/수집 인프라 위에 스크리닝(12가지 필터), 재무제표(분석형), 섹터/산업 분석(혼합 시각화) 기능을 추가한다.

## Data Model

### 새로 추가하는 테이블

| 테이블 | 용도 | 주요 컬럼 |
|--------|------|-----------|
| `financials` | 재무제표 | stock_id, period(2024Q1 등), type(annual/quarterly), revenue, operating_income, net_income, total_assets, total_liabilities, total_equity, operating_cash_flow, extra_data(JSONB) |
| `stock_metrics` | 스크리닝용 일일 스냅샷 | stock_id, date, per, pbr, dividend_yield, week52_high, week52_low, avg_volume_30d, revenue_growth, operating_margin, roe, debt_ratio |
| `sector_metrics` | 섹터별 일일 집계 | sector_id, date, avg_per, avg_pbr, total_market_cap, avg_dividend_yield, top_gainers(JSONB), top_losers(JSONB) |
| `industry_metrics` | 산업별 일일 집계 | industry_id, date, avg_per, avg_pbr, total_market_cap, stock_count, performance_1d, performance_1w, performance_1m, performance_3m, performance_1y |

### 관계

- `stocks` 1:N `financials`
- `stocks` 1:N `stock_metrics` (일별)
- `sectors` 1:N `sector_metrics` (일별)
- `industries` 1:N `industry_metrics` (일별)

### 설계 포인트

- `stock_metrics`: `(stock_id, date)` 복합 인덱스 + 각 필터 컬럼에 개별 인덱스
- `financials`: `(stock_id, period)` 유니크 제약 + `extra_data JSONB`로 세부 항목 유연하게 저장
- `sector_metrics`, `industry_metrics`: `(sector_id/industry_id, date)` 복합 인덱스

## API Design

### 스크리닝

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/screening?sector=&minPer=&maxPer=&minPbr=&maxPbr=&minCap=&maxCap=&minDividendYield=&minRoe=&maxDebtRatio=&minRevenueGrowth=&minOperatingMargin=&exchange=&page=&size=&sort=` | 12가지 필터 + 정렬 + 페이징 |

필터 조건 (모두 선택적):
1. sector — 섹터 ID
2. minPer / maxPer — PER 범위
3. minPbr / maxPbr — PBR 범위
4. minCap / maxCap — 시가총액 범위
5. minDividendYield — 최소 배당수익률
6. minRoe — 최소 ROE
7. maxDebtRatio — 최대 부채비율
8. minRevenueGrowth — 최소 매출성장률
9. minOperatingMargin — 최소 영업이익률
10. exchange — 거래소 (NYSE, NASDAQ)

정렬: `sort=marketCap,desc` 형식. 기본: 시가총액 내림차순.
페이징: `page`, `size` 파라미터. 기본 20건.

### 재무제표

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/stocks/{ticker}/financials?type=annual` | 연간 또는 분기 재무제표 |
| GET | `/api/stocks/{ticker}/financials/ratios` | 재무비율 + 섹터 평균 대비 비교 |

`/financials` 응답:
```json
{
  "success": true,
  "data": [
    {
      "period": "2025",
      "type": "annual",
      "revenue": 394328000000,
      "operatingIncome": 114301000000,
      "netIncome": 96995000000,
      "totalAssets": 352583000000,
      "totalLiabilities": 290437000000,
      "totalEquity": 62146000000,
      "operatingCashFlow": 110543000000,
      "extraData": {}
    }
  ]
}
```

`/financials/ratios` 응답:
```json
{
  "success": true,
  "data": {
    "stock": {
      "roe": 0.285,
      "roa": 0.156,
      "operatingMargin": 0.290,
      "netMargin": 0.246,
      "debtRatio": 4.673,
      "currentRatio": 1.07,
      "revenueGrowth": 0.082
    },
    "sectorAverage": {
      "roe": 0.195,
      "roa": 0.102,
      "operatingMargin": 0.215,
      "netMargin": 0.178,
      "debtRatio": 2.150,
      "currentRatio": 1.45,
      "revenueGrowth": 0.065
    }
  }
}
```

### 섹터/산업

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/sectors` | 전체 섹터 + 요약 지표 |
| GET | `/api/sectors/{id}` | 섹터 상세 (소속 산업 목록, 지표) |
| GET | `/api/sectors/{id}/performance?period=1m` | 섹터 기간별 퍼포먼스 |
| GET | `/api/sectors/{id}/rankings?sort=gainers` | 섹터 내 종목 랭킹 (상승/하락/시가총액) |
| GET | `/api/industries/{id}` | 산업 상세 + 소속 종목 리스트 |
| GET | `/api/sectors/compare` | 전체 섹터 비교 (히트맵 데이터) |

## Backend

### 새로 추가하는 클래스

```
com.stockanalyzer
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
│   ├── ScreeningResult.java
│   ├── ScreeningFilter.java
│   ├── SectorOverview.java
│   ├── SectorDetail.java
│   ├── SectorPerformance.java
│   └── IndustryDetail.java
├── service/
│   ├── ScreeningService.java
│   ├── FinancialService.java
│   └── SectorService.java
├── controller/
│   ├── ScreeningController.java
│   └── SectorController.java
└── scheduler/
    ├── FinancialCollector.java
    └── MetricsCalculator.java
```

### 기존 파일 수정

- `StockController.java`: `/api/stocks/{ticker}/financials` 엔드포인트 추가 (FinancialService 위임)
- `StockDetail.java` (DTO): 재무비율 관련 필드는 별도 DTO(FinancialRatios)로 분리

### 스크리닝 쿼리 전략

`StockMetricRepository`에서 동적 쿼리를 구성한다. JPA Criteria API를 사용하여 선택적 필터 조건을 AND로 조합:

```java
public List<StockMetric> findByFilters(ScreeningFilter filter, int page, int size, String sort) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<StockMetric> cq = cb.createQuery(StockMetric.class);
    Root<StockMetric> root = cq.from(StockMetric.class);
    List<Predicate> predicates = new ArrayList<>();

    // 최신 날짜의 스냅샷만 조회
    predicates.add(cb.equal(root.get("date"), getLatestMetricDate()));

    if (filter.getSectorId() != null) {
        predicates.add(cb.equal(root.get("stock").get("industry").get("sector").get("id"), filter.getSectorId()));
    }
    if (filter.getMinPer() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("per"), filter.getMinPer()));
    }
    // ... 나머지 필터 조건
    cq.where(predicates.toArray(new Predicate[0]));
    // 정렬 + 페이징 적용
}
```

## Frontend

### 새로 추가하는 파일

| 파일 | 역할 |
|------|------|
| `pages/Screening.tsx` | 좌측 필터 패널 + 우측 결과 테이블 (정렬/페이징) |
| `pages/SectorAnalysis.tsx` | 상단: 히트맵 + 퍼포먼스 차트. 하단: 섹터 테이블. 클릭 → 드릴다운 |
| `pages/IndustryDetail.tsx` | 산업 상세 + 소속 종목 리스트 |
| `components/FilterPanel.tsx` | 스크리닝 필터 UI (범위 입력, 섹터 셀렉트) |
| `components/SectorHeatmap.tsx` | Recharts Treemap 기반 섹터 히트맵 |
| `components/PerformanceChart.tsx` | 섹터/산업 퍼포먼스 바 차트 |
| `components/FinancialTable.tsx` | 재무제표 탭 (손익/대차/현금흐름 테이블) |
| `components/RatioComparison.tsx` | 재무비율 vs 섹터 평균 바 차트 |

### 기존 파일 수정

| 파일 | 변경 |
|------|------|
| `App.tsx` | `/screening`, `/sectors`, `/sectors/:id`, `/industries/:id` 라우트 추가 |
| `StockDetail.tsx` | 차트 아래에 재무제표 탭 영역 추가 (FinancialTable + RatioComparison) |
| `api/stockApi.ts` | 스크리닝, 재무, 섹터 API 함수 추가 |

### StockDetail 재무제표 탭 구성

기존 주가 차트 아래에 탭 UI 추가:
- 탭 1 요약: 핵심 지표 카드 (매출, 순이익, 영업이익) + 연도별 트렌드 라인 차트 (Recharts)
- 탭 2 손익계산서: 연도별 테이블
- 탭 3 대차대조표: 연도별 테이블
- 탭 4 현금흐름표: 연도별 테이블
- 탭 5 재무비율: ROE, ROA, 영업이익률 등 + 섹터 평균 대비 비교 바 차트

## Schedulers

| 스케줄러 | 크론 | 동작 |
|---------|------|------|
| `FinancialCollector` | `0 0 7 ? * SUN` (일요일 07:00) | Yahoo Finance에서 연간/분기 재무데이터 수집 → `financials` 저장 |
| `MetricsCalculator` | `0 30 7 ? * TUE-SAT` (장 마감 후 07:30) | `stock_metrics` 계산 (12가지 지표), `sector_metrics` 집계, `industry_metrics` 집계 |

### MetricsCalculator 계산 로직

stock_metrics 각 항목:
- PER: 현재가 / (최근 4분기 순이익 합 ÷ 발행주식수). Yahoo quote에서 가져온 값 우선 사용.
- PBR: 현재가 / ((총자산 - 총부채) ÷ 발행주식수). Yahoo quote에서 가져온 값 우선 사용.
- 배당수익률: Yahoo Finance quote에서 직접 가져옴
- 52주 최고/최저: daily_prices에서 최근 252거래일 max(high) / min(low)
- 30일 평균 거래량: daily_prices에서 최근 30일 avg(volume)
- 매출성장률: (최근 연매출 - 전년 연매출) / 전년 연매출
- 영업이익률: 영업이익 / 매출
- ROE: 순이익 / 자기자본
- 부채비율: 총부채 / 자기자본

sector_metrics / industry_metrics: 소속 종목의 stock_metrics 기반 평균/합계 집계.

실행 순서: PriceCollector(07:00) → MetricsCalculator(07:30). 크론 시간 차이로 순서 보장.
