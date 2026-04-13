# Stock Analyzer 백엔드 배포 수정 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 백엔드 API가 404를 반환하는 문제를 해결하고, DB를 생성하여 종목 데이터를 수집할 수 있도록 한다.

**Architecture:** Servlet API 충돌과 컴포넌트 스캔 중복을 수정하고, PostgreSQL에 stockanalyzer DB를 생성한 후, Docker 이미지를 재빌드하여 Synology에 배포한다. 이후 초기 데이터 수집을 실행하고, 검색 폴백을 추가한다.

**Tech Stack:** Spring MVC 4.3, Hibernate 5.4, PostgreSQL, Docker, Tomcat 9 WAR

---

### Task 1: 백엔드 코드 버그 수정

**Files:**
- Modify: `backend/build.gradle:30` — servlet API를 `compileOnly`로 변경
- Modify: `backend/src/main/java/com/stockanalyzer/config/AppConfig.java:25` — controller 패키지 스캔 제외
- Modify: `backend/src/main/java/com/stockanalyzer/config/WebConfig.java:22-23` — CORS 와일드카드 수정

- [ ] **Step 1: build.gradle — servlet API를 compileOnly로 변경**

`backend/build.gradle:30`에서:
```gradle
# AS-IS
implementation 'javax.servlet:javax.servlet-api:3.1.0'

# TO-BE
compileOnly 'javax.servlet:javax.servlet-api:3.1.0'
```

참고: `EmbeddedTomcat`은 `tomcat-embed-core`가 servlet API를 포함하므로 로컬 `gradle run`에도 문제없음.

- [ ] **Step 2: AppConfig — controller 이중 스캔 방지**

`backend/src/main/java/com/stockanalyzer/config/AppConfig.java:25`에서:
```java
// AS-IS
@ComponentScan(basePackages = "com.stockanalyzer")

// TO-BE
@ComponentScan(basePackages = "com.stockanalyzer",
    excludeFilters = @ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.REGEX,
        pattern = "com\\.stockanalyzer\\.controller\\..*"))
```

import 추가 필요: `org.springframework.context.annotation.FilterType` (이미 `@ComponentScan`과 같은 패키지)

- [ ] **Step 3: WebConfig — CORS 와일드카드 수정**

Spring 4.3은 `*.vercel.app` 패턴을 지원하지 않음. 구체적 origin 또는 `*`로 변경:
```java
// AS-IS
.allowedOrigins("http://localhost:5173", "https://*.vercel.app", "http://fumarole.synology.me:39091")

// TO-BE
.allowedOrigins("http://localhost:5173", "http://fumarole.synology.me:39091", "*")
```

참고: 이 앱은 인증이 없는 공개 API이므로 `*`가 적절. 인증이 추가되면 구체적 origin으로 다시 변경 필요.

- [ ] **Step 4: 로컬 빌드 확인**

Run: `cd backend && gradle war -x test --no-daemon`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add backend/build.gradle backend/src/main/java/com/stockanalyzer/config/AppConfig.java backend/src/main/java/com/stockanalyzer/config/WebConfig.java
git commit -m "fix: resolve servlet API conflict, component scan overlap, and CORS wildcard"
```

---

### Task 2: stockanalyzer DB 생성 + docker-compose 수정

**Files:**
- Modify: `docker-compose.synology.yml:13` — DB_URL 변경

- [ ] **Step 1: PostgreSQL에 stockanalyzer DB 생성**

Mac에서 Python으로 실행:
```python
import psycopg2
conn = psycopg2.connect(host='fumarole.synology.me', port=35435,
    dbname='postgres', user='tiger', password='Freewalk345^', sslmode='require')
conn.autocommit = True
cur = conn.cursor()
cur.execute("CREATE DATABASE stockanalyzer OWNER tiger")
conn.close()
```

Expected: DB 생성 성공. 이미 존재하면 에러 — 그 경우 스킵.

- [ ] **Step 2: DB 생성 확인**

```python
conn = psycopg2.connect(host='fumarole.synology.me', port=35435,
    dbname='stockanalyzer', user='tiger', password='Freewalk345^', sslmode='require')
cur = conn.cursor()
cur.execute("SELECT 1")
print(cur.fetchone())  # (1,)
conn.close()
```

- [ ] **Step 3: docker-compose.synology.yml DB_URL 수정**

```yaml
# AS-IS
DB_URL: jdbc:postgresql://172.17.0.1:5433/axidiary?sslmode=disable

# TO-BE
DB_URL: jdbc:postgresql://172.17.0.1:5433/stockanalyzer?sslmode=disable
```

- [ ] **Step 4: 커밋**

```bash
git add docker-compose.synology.yml
git commit -m "fix: point stock-api to dedicated stockanalyzer database"
```

---

### Task 3: Synology에 Docker 재배포

**Prerequisites:** Task 1, Task 2 완료. SSH 접속 가능해야 함.

- [ ] **Step 1: 프로젝트 파일을 Synology로 전송**

SSH 파이프를 사용하여 필요한 파일 전송:
```bash
# backend 디렉토리와 docker-compose를 tar로 묶어 전송
tar czf - backend/ docker-compose.synology.yml | \
  sshpass -p 'free5walk%' ssh -o StrictHostKeyChecking=no -p 30025 \
  fumarole@fumarole.synology.me "cat > /tmp/stock-app.tar.gz"
```

- [ ] **Step 2: Synology에서 압축 해제 및 Docker 재빌드**

```bash
sshpass -p 'free5walk%' ssh -o StrictHostKeyChecking=no -p 30025 \
  fumarole@fumarole.synology.me '
  cd /tmp && tar xzf stock-app.tar.gz &&
  printf "free5walk%%\n" | sudo -S /usr/local/bin/docker compose \
    -f docker-compose.synology.yml up -d --build stock-api
'
```

(참고: SSH sudo 접속이 안 될 경우 DSM 웹 UI에서 Docker 컨테이너 수동 재시작 필요)

- [ ] **Step 3: API 응답 확인**

```bash
# 30초 대기 후 (Tomcat + Spring 초기화)
curl -s "http://fumarole.synology.me:39091/api/dashboard/indices" | head -100
```

Expected: JSON 응답 (`{"success":true,"data":[...]}`) — 404가 아닌 정상 응답

- [ ] **Step 4: DB 테이블 자동 생성 확인**

```python
import psycopg2
conn = psycopg2.connect(host='fumarole.synology.me', port=35435,
    dbname='stockanalyzer', user='tiger', password='Freewalk345^', sslmode='require')
cur = conn.cursor()
cur.execute("SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename")
for row in cur.fetchall():
    print(row[0])
conn.close()
```

Expected: `stocks`, `sectors`, `industries`, `daily_prices`, `financials` 등 테이블 존재

---

### Task 4: 초기 데이터 수집 실행

**Prerequisites:** Task 3 완료, API 정상 응답 확인.

- [ ] **Step 1: 초기 수집 트리거**

```bash
curl -s -X POST "http://fumarole.synology.me:39091/api/admin/collect"
```

Expected: `{"success":true,"data":"Collection started in background"}`

이 작업은 40개 종목을 Yahoo Finance에서 수집하며 약 2분 소요 (종목당 2초 sleep).

- [ ] **Step 2: 수집 완료 대기 후 확인 (약 2-3분 후)**

```bash
curl -s "http://fumarole.synology.me:39091/api/stocks?q=AAPL"
```

Expected: `{"success":true,"data":[{"ticker":"AAPL","companyName":"Apple Inc.",...}]}`

- [ ] **Step 3: DB에 종목 수 확인**

```python
import psycopg2
conn = psycopg2.connect(host='fumarole.synology.me', port=35435,
    dbname='stockanalyzer', user='tiger', password='Freewalk345^', sslmode='require')
cur = conn.cursor()
cur.execute("SELECT COUNT(*) FROM stocks")
print("Stock count:", cur.fetchone()[0])
cur.execute("SELECT COUNT(*) FROM daily_prices")
print("Price count:", cur.fetchone()[0])
conn.close()
```

Expected: stocks ≈ 40, daily_prices > 0

---

### Task 5: Yahoo Finance 검색 폴백 추가

**Files:**
- Modify: `backend/src/main/java/com/stockanalyzer/client/YahooFinanceClient.java` — `searchTickers` 메서드 추가
- Modify: `backend/src/main/java/com/stockanalyzer/service/StockService.java:33-36` — 검색 폴백 로직
- Modify: `backend/src/main/java/com/stockanalyzer/dto/StockSearchResult.java` — 생성자 확인

- [ ] **Step 1: YahooFinanceClient에 검색 메서드 추가**

Yahoo Finance v1 search API를 사용하여 티커 검색:

`backend/src/main/java/com/stockanalyzer/client/YahooFinanceClient.java`에 메서드 추가:
```java
public List<StockDetail> searchTickers(String query) {
    String url = "https://query1.finance.yahoo.com/v1/finance/search?q=" + query + "&quotesCount=10&newsCount=0";
    try {
        String json = fetchWithHeaders(url);
        JsonNode root = readTreeExact(json);
        JsonNode quotes = root.path("quotes");
        List<StockDetail> results = new ArrayList<>();
        for (JsonNode q : quotes) {
            if (!"EQUITY".equals(q.path("quoteType").asText())) continue;
            StockDetail d = new StockDetail();
            d.setTicker(q.path("symbol").asText());
            d.setCompanyName(q.has("longname") ? q.path("longname").asText() : q.path("shortname").asText());
            d.setExchange(q.path("exchDisp").asText());
            results.add(d);
        }
        return results;
    } catch (Exception e) {
        log.warn("Yahoo search failed for '{}': {}", query, e.getMessage());
        return new ArrayList<>();
    }
}
```

- [ ] **Step 2: StockService.search()에 폴백 추가**

`backend/src/main/java/com/stockanalyzer/service/StockService.java`의 `search` 메서드:
```java
// AS-IS
public List<StockSearchResult> search(String keyword) {
    return stockRepository.search(keyword, 10).stream()
            .map(s -> new StockSearchResult(s.getTicker(), s.getCompanyName(), s.getExchange()))
            .collect(Collectors.toList());
}

// TO-BE
public List<StockSearchResult> search(String keyword) {
    List<StockSearchResult> dbResults = stockRepository.search(keyword, 10).stream()
            .map(s -> new StockSearchResult(s.getTicker(), s.getCompanyName(), s.getExchange()))
            .collect(Collectors.toList());
    if (!dbResults.isEmpty()) {
        return dbResults;
    }
    return yahooClient.searchTickers(keyword).stream()
            .map(d -> new StockSearchResult(d.getTicker(), d.getCompanyName(), d.getExchange()))
            .collect(Collectors.toList());
}
```

DB에 결과가 있으면 DB 결과 반환, 없으면 Yahoo Finance 실시간 검색.

- [ ] **Step 3: 로컬 빌드 확인**

Run: `cd backend && gradle war -x test --no-daemon`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/stockanalyzer/client/YahooFinanceClient.java \
       backend/src/main/java/com/stockanalyzer/service/StockService.java
git commit -m "feat: add Yahoo Finance search fallback for stocks not in DB"
```

- [ ] **Step 5: Synology 재배포 (Task 3의 Step 1~3 반복)**

재배포 후 확인:
```bash
curl -s "http://fumarole.synology.me:39091/api/stocks?q=TSMC"
```

Expected: DB에 TSMC가 없어도 Yahoo Finance에서 검색 결과 반환.

---

## 검증 체크리스트

완료 후 아래 전부 통과해야 함:

- [ ] `curl /api/dashboard/indices` → 200 + JSON
- [ ] `curl /api/stocks?q=AAPL` → AAPL 검색 결과
- [ ] `curl /api/stocks?q=TSMC` → Yahoo 폴백으로 검색 결과
- [ ] `curl /api/stocks/AAPL` → 종목 상세 정보
- [ ] `curl /api/screening?page=0&size=5` → 스크리닝 결과
- [ ] Vercel 프론트엔드에서 종목 검색 → 드롭다운 표시 → 클릭 시 상세 페이지 정상
