# US Stock Analyzer Platform — Design Spec

## Overview

미국 개별주식 데이터를 수집, 저장, 분석하여 제공하는 종합 주식 분석 웹 플랫폼.
실제 서비스 운영을 목적으로 하며, 인증 없는 공개 서비스로 제공한다.

## Architecture

### 시스템 구성

- **프론트엔드**: React SPA → Vercel 배포
- **백엔드**: Spring 4.x 모놀리식 앱 → Synology NAS (Docker)
- **데이터베이스**: PostgreSQL → Synology NAS (Docker)
- **외부 데이터**: 무료 API (Yahoo Finance, Alpha Vantage, Finnhub/NewsAPI, SEC EDGAR)

### 통신

- 프론트엔드 ↔ 백엔드: REST API (JSON)
- 백엔드 → 외부 API: 스케줄러가 주기적으로 호출하여 DB에 저장

## Data Model

### 테이블

| 테이블 | 용도 | 주요 컬럼 |
|--------|------|-----------|
| `stocks` | 종목 기본정보 | ticker, company_name, sector_id, industry_id, market_cap, exchange |
| `daily_prices` | 일별 시세 | stock_id, date, open, high, low, close, volume, adjusted_close |
| `financials` | 재무제표 | stock_id, period, type(연/분기), revenue, net_income, total_assets, total_debt, cash_flow 등 |
| `news` | 뉴스/공시 | title, source, url, published_at, summary |
| `stock_news` | 종목-뉴스 조인 테이블 | stock_id, news_id |
| `sectors` | 섹터 분류 | name, description |
| `industries` | 산업 분류 (섹터 하위) | sector_id, name, description |
| `sector_metrics` | 섹터별 집계 지표 | sector_id, date, avg_per, avg_pbr, total_market_cap, avg_dividend_yield, top_gainers(JSONB), top_losers(JSONB) |
| `industry_metrics` | 산업별 집계 지표 | industry_id, date, avg_per, avg_pbr, total_market_cap, stock_count, performance_1d/1w/1m/3m/1y |
| `technical_indicators` | 기술적 지표 캐시 | stock_id, date, indicator_type, value(JSON) |

### 관계

- `sectors` 1:N `industries` 1:N `stocks`
- `sectors` 1:N `sector_metrics` (일별 집계)
- `industries` 1:N `industry_metrics` (일별 집계)
- `stocks` 1:N `daily_prices`
- `stocks` 1:N `financials`
- `stocks` N:M `news` (조인 테이블 필요)

### 설계 포인트

- `daily_prices`: 데이터량이 가장 큼 → `(stock_id, date)` 복합 인덱스 + 파티셔닝 고려
- `financials`: 핵심 컬럼 + `extra_data JSONB` 구조로 유연성 확보
- `technical_indicators`: 캐시 성격 → TTL 기반 갱신
- 섹터/산업 집계: 스케줄러가 매일 장 마감 후 계산하여 저장

## API Design

### REST Endpoints

| 분류 | 메서드 | 엔드포인트 | 설명 |
|------|--------|-----------|------|
| 종목 | GET | `/api/stocks?q={keyword}` | 종목 검색 (티커/회사명) |
| | GET | `/api/stocks/{ticker}` | 종목 상세 정보 |
| | GET | `/api/stocks/{ticker}/prices?period={1m,3m,1y,5y}` | 기간별 시세 데이터 |
| | GET | `/api/stocks/{ticker}/financials?type={annual,quarterly}` | 재무제표 |
| | GET | `/api/stocks/{ticker}/news` | 종목 관련 뉴스 |
| | GET | `/api/stocks/{ticker}/indicators` | 기술적 지표 |
| 스크리닝 | GET | `/api/screening?sector=&minPer=&maxPer=&minCap=&...` | 조건별 종목 필터링 |
| 비교 | GET | `/api/compare?tickers=AAPL,MSFT,GOOGL` | 종목 비교 데이터 |
| 섹터/산업 | GET | `/api/sectors` | 전체 섹터 목록 + 요약 지표 |
| | GET | `/api/sectors/{id}` | 섹터 상세 (산업 목록, 지표) |
| | GET | `/api/sectors/{id}/performance?period={1d,1w,1m,3m,1y}` | 섹터 퍼포먼스 |
| | GET | `/api/sectors/{id}/rankings` | 섹터 내 종목 랭킹 |
| | GET | `/api/industries/{id}` | 산업 상세 + 소속 종목 |
| | GET | `/api/sectors/compare` | 섹터 간 비교 |

### 공통 규칙

- 페이징: `page`, `size` 파라미터. 기본 20건.
- 응답 형식: 통일된 JSON wrapper `{ "success": true, "data": {...}, "meta": {...} }`

## Frontend

### 기술 스택

- React + TypeScript
- TradingView Lightweight Charts (주가 차트)
- Recharts (섹터 히트맵, 바 차트 등 일반 차트)
- Tailwind CSS (스타일링)
- React Router (페이지 라우팅)
- Axios (API 호출)

### 레이아웃

사이드바 네비게이션 + 콘텐츠 영역 구조.
좌측에 메뉴 항상 노출, 사이드바 상단에 글로벌 종목 검색창.

### 글로벌 종목 검색

- 사이드바 상단에 위치
- 2글자 이상 입력 시 자동완성 (티커 + 회사명 동시 검색)
- 엔터 또는 클릭 → 종목 상세 페이지로 이동
- 종목 비교 페이지에서는 태그 형태로 2~5개 입력 가능

### 주요 페이지

| 페이지 | 주요 구성요소 |
|--------|-------------|
| 대시보드 | 주요 지수 요약, 섹터 히트맵, 오늘의 상승/하락 Top 5, 최근 뉴스 |
| 종목 상세 | 주가 차트(캔들/라인), 기본 정보, 기술적 지표, 재무제표 탭, 관련 뉴스, 섹터 평균 대비 비교 |
| 스크리닝 | 필터 패널(섹터, PER, 시가총액 등) + 결과 테이블(정렬/페이징) |
| 섹터 분석 | 섹터별 퍼포먼스 차트, 섹터 → 산업 드릴다운 → 종목 리스트, 섹터 간 비교 |
| 종목 비교 | 티커 2~5개 입력, 가격 수익률 차트 오버레이, 재무 지표 테이블 비교 |
| 뉴스 | 전체/섹터별/종목별 뉴스 피드, SEC 공시 목록 |

## Backend (Spring 4.x)

### 패키지 구조

```
com.stockanalyzer
├── config/           # Spring 설정 (DataSource, Scheduler, CORS 등)
├── controller/       # REST Controller
│   ├── StockController
│   ├── SectorController
│   ├── ScreeningController
│   ├── CompareController
│   └── NewsController
├── service/          # 비즈니스 로직
│   ├── StockService
│   ├── SectorService
│   ├── ScreeningService
│   ├── FinancialService
│   ├── NewsService
│   └── TechnicalIndicatorService
├── repository/       # JPA Repository
├── entity/           # JPA Entity
├── dto/              # 요청/응답 DTO
├── scheduler/        # 데이터 수집 스케줄러
│   ├── PriceCollector
│   ├── FinancialCollector
│   ├── NewsCollector
│   └── SectorMetricsCalculator
├── client/           # 외부 API 호출 클라이언트
│   ├── YahooFinanceClient
│   ├── AlphaVantageClient
│   └── NewsApiClient
└── common/           # 공통 유틸, 응답 wrapper, 예외 처리
```

### 핵심 기술 결정

- Spring MVC 4.x + Java Config
- JPA (Hibernate) — Entity 매핑, Repository 패턴
- Spring Task Scheduler — `@Scheduled`로 데이터 수집 (cron 표현식)
- RestTemplate — 외부 API 호출
- 빌드 도구: Gradle

### 스케줄러 실행 주기

- 가격 수집: 평일 장 마감 후 1일 1회
- 재무 데이터: 주 1회 (분기 공시 반영)
- 뉴스: 매 6시간
- 섹터/산업 집계: 가격 수집 완료 후 1일 1회

## Deployment

### Docker 구성 (Synology)

```yaml
# docker-compose.yml
services:
  spring-app:
    image: openjdk:11
    ports: ["8080:8080"]
    environment: DB 접속정보, API 키
    volumes: 로그 저장
    memory: 1024m

  postgres:
    image: postgres:15
    ports: ["5432:5432"]
    volumes: 데이터 영속화
    초기 스키마 자동 실행
```

### 네트워크

- Synology 리버스 프록시 + Let's Encrypt HTTPS
- DDNS 또는 고정 IP로 외부 접근
- CORS: Vercel 도메인 허용

### Vercel (프론트엔드)

- GitHub 연동 자동 배포
- 환경변수로 백엔드 API URL 설정

### 운영 고려사항

- JVM 메모리 제한: `-Xmx1g` (Synology 리소스 한계)
- 무료 API rate limiting 처리
- DB 백업: pg_dump 크론 스케줄

## Release Phases

| 단계 | 범위 | 목표 |
|------|------|------|
| Phase 1 | 종목 검색 + 기본 정보 + 주가 차트 + 데이터 수집 파이프라인 | 핵심 인프라 완성 |
| Phase 2 | 스크리닝 + 재무제표 + 섹터 분석 | 분석 기능 확장 |
| Phase 3 | 뉴스/공시 + 종목 비교 + 대시보드 고도화 | 전체 기능 완성 |
