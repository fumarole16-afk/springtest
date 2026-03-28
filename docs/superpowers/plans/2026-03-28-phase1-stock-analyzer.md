# Phase 1: Stock Analyzer — 핵심 인프라 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 미국 주식 종목 검색, 기본 정보 조회, 주가 차트 표시, 그리고 Yahoo Finance에서 데이터를 자동 수집하는 파이프라인을 구축한다.

**Architecture:** Spring Framework 4.3.x 모놀리식 백엔드가 REST API를 제공하고, 스케줄러로 Yahoo Finance에서 데이터를 수집하여 PostgreSQL에 저장한다. React + TypeScript 프론트엔드는 사이드바 레이아웃으로 종목 검색/상세/차트를 제공하며 Vercel에 배포한다.

**Tech Stack:** Spring MVC 4.3.x, JPA (Hibernate 5.x), PostgreSQL 15, Gradle, React 18, TypeScript, TradingView Lightweight Charts, Tailwind CSS, Docker

---

## File Structure

### Backend (`backend/`)

```
backend/
├── build.gradle
├── settings.gradle
├── Dockerfile
├── src/main/java/com/stockanalyzer/
│   ├── config/
│   │   ├── AppConfig.java              # DataSource, JPA, Transaction 설정
│   │   ├── WebConfig.java              # MVC, CORS, Jackson 설정
│   │   └── WebAppInitializer.java      # DispatcherServlet 초기화 (web.xml 대체)
│   ├── common/
│   │   └── ApiResponse.java            # 통일된 JSON 응답 wrapper
│   ├── entity/
│   │   ├── Sector.java
│   │   ├── Industry.java
│   │   ├── Stock.java
│   │   └── DailyPrice.java
│   ├── repository/
│   │   ├── SectorRepository.java
│   │   ├── IndustryRepository.java
│   │   ├── StockRepository.java
│   │   └── DailyPriceRepository.java
│   ├── dto/
│   │   ├── StockSearchResult.java
│   │   ├── StockDetail.java
│   │   └── PriceData.java
│   ├── service/
│   │   └── StockService.java
│   ├── controller/
│   │   └── StockController.java
│   ├── client/
│   │   └── YahooFinanceClient.java
│   └── scheduler/
│       └── PriceCollector.java
├── src/main/resources/
│   ├── application.properties
│   └── data/
│       └── sectors-industries.sql      # GICS 섹터/산업 초기 데이터
└── src/test/java/com/stockanalyzer/
    ├── repository/
    │   ├── StockRepositoryTest.java
    │   └── DailyPriceRepositoryTest.java
    ├── service/
    │   └── StockServiceTest.java
    ├── client/
    │   └── YahooFinanceClientTest.java
    └── controller/
        └── StockControllerTest.java
```

### Frontend (`frontend/`)

```
frontend/
├── package.json
├── tsconfig.json
├── tailwind.config.js
├── src/
│   ├── App.tsx
│   ├── main.tsx
│   ├── api/
│   │   └── stockApi.ts
│   ├── components/
│   │   ├── Layout.tsx               # 사이드바 + 콘텐츠 레이아웃
│   │   ├── Sidebar.tsx
│   │   ├── StockSearch.tsx          # 자동완성 검색
│   │   └── PriceChart.tsx           # TradingView Lightweight Charts
│   └── pages/
│       ├── Dashboard.tsx
│       └── StockDetail.tsx
└── .env
```

### Infrastructure

```
docker-compose.yml                     # spring-app + postgres
```

---

## Task 1: Gradle 프로젝트 스캐폴딩

**Files:**
- Create: `backend/build.gradle`
- Create: `backend/settings.gradle`
- Create: `backend/src/main/java/com/stockanalyzer/config/AppConfig.java`
- Create: `backend/src/main/java/com/stockanalyzer/config/WebConfig.java`
- Create: `backend/src/main/java/com/stockanalyzer/config/WebAppInitializer.java`
- Create: `backend/src/main/resources/application.properties`

- [ ] **Step 1: build.gradle 작성**

```groovy
// backend/build.gradle
plugins {
    id 'java'
    id 'war'
}

group = 'com.stockanalyzer'
version = '1.0.0'
sourceCompatibility = '11'

repositories {
    mavenCentral()
}

dependencies {
    // Spring Framework 4.3
    implementation 'org.springframework:spring-webmvc:4.3.30.RELEASE'
    implementation 'org.springframework:spring-orm:4.3.30.RELEASE'
    implementation 'org.springframework:spring-context-support:4.3.30.RELEASE'

    // JPA + Hibernate
    implementation 'org.hibernate:hibernate-core:5.4.33.Final'
    implementation 'org.hibernate:hibernate-entitymanager:5.4.33.Final'

    // PostgreSQL
    implementation 'org.postgresql:postgresql:42.7.3'

    // HikariCP connection pool
    implementation 'com.zaxxer:HikariCP:4.0.3'

    // Jackson JSON
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.13.5'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.5'

    // Servlet API
    providedCompile 'javax.servlet:javax.servlet-api:3.1.0'

    // Logging
    implementation 'org.slf4j:slf4j-api:1.7.36'
    implementation 'ch.qos.logback:logback-classic:1.2.12'

    // Test
    testImplementation 'org.springframework:spring-test:4.3.30.RELEASE'
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'com.h2database:h2:1.4.200'
    testImplementation 'org.mockito:mockito-core:3.12.4'
}

test {
    useJUnit()
}
```

```groovy
// backend/settings.gradle
rootProject.name = 'stock-analyzer'
```

- [ ] **Step 2: application.properties 작성**

```properties
# backend/src/main/resources/application.properties
db.url=jdbc:postgresql://localhost:5432/stockanalyzer
db.username=stockuser
db.password=stockpass
db.driver=org.postgresql.Driver

hibernate.dialect=org.hibernate.dialect.PostgreSQL95Dialect
hibernate.hbm2ddl.auto=update
hibernate.show_sql=false

yahoo.finance.base-url=https://query1.finance.yahoo.com
yahoo.finance.rate-limit-per-hour=500
```

- [ ] **Step 3: WebAppInitializer 작성**

```java
// backend/src/main/java/com/stockanalyzer/config/WebAppInitializer.java
package com.stockanalyzer.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{AppConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{WebConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }
}
```

- [ ] **Step 4: AppConfig 작성 (DataSource, JPA, Transaction)**

```java
// backend/src/main/java/com/stockanalyzer/config/AppConfig.java
package com.stockanalyzer.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = "com.stockanalyzer")
@PropertySource("classpath:application.properties")
@EnableTransactionManagement
@EnableScheduling
public class AppConfig {

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.username}")
    private String dbUsername;

    @Value("${db.password}")
    private String dbPassword;

    @Value("${db.driver}")
    private String dbDriver;

    @Value("${hibernate.dialect}")
    private String hibernateDialect;

    @Value("${hibernate.hbm2ddl.auto}")
    private String hbm2ddl;

    @Value("${hibernate.show_sql}")
    private String showSql;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName(dbDriver);
        ds.setJdbcUrl(dbUrl);
        ds.setUsername(dbUsername);
        ds.setPassword(dbPassword);
        ds.setMaximumPoolSize(10);
        return ds;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.stockanalyzer.entity");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties props = new Properties();
        props.setProperty("hibernate.dialect", hibernateDialect);
        props.setProperty("hibernate.hbm2ddl.auto", hbm2ddl);
        props.setProperty("hibernate.show_sql", showSql);
        em.setJpaProperties(props);

        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
```

- [ ] **Step 5: WebConfig 작성 (MVC, CORS, Jackson)**

```java
// backend/src/main/java/com/stockanalyzer/config/WebConfig.java
package com.stockanalyzer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "https://*.vercel.app")
                .allowedMethods("GET")
                .maxAge(3600);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converters.add(new MappingJackson2HttpMessageConverter(mapper));
    }
}
```

- [ ] **Step 6: 디렉토리 구조 생성 및 빌드 확인**

Run: `cd backend && gradle build`
Expected: BUILD SUCCESSFUL (컴파일 성공, 테스트는 아직 없으므로 스킵)

- [ ] **Step 7: 커밋**

```bash
git add backend/
git commit -m "feat: scaffold Spring 4.3 project with Gradle, JPA, and PostgreSQL config"
```

---

## Task 2: Entity + Repository — Sector, Industry

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/entity/Sector.java`
- Create: `backend/src/main/java/com/stockanalyzer/entity/Industry.java`
- Create: `backend/src/main/java/com/stockanalyzer/repository/SectorRepository.java`
- Create: `backend/src/main/java/com/stockanalyzer/repository/IndustryRepository.java`
- Create: `backend/src/main/resources/data/sectors-industries.sql`

- [ ] **Step 1: Sector Entity 작성**

```java
// backend/src/main/java/com/stockanalyzer/entity/Sector.java
package com.stockanalyzer.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sectors")
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @OneToMany(mappedBy = "sector")
    private List<Industry> industries = new ArrayList<>();

    public Sector() {}

    public Sector(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Industry> getIndustries() { return industries; }
    public void setIndustries(List<Industry> industries) { this.industries = industries; }
}
```

- [ ] **Step 2: Industry Entity 작성**

```java
// backend/src/main/java/com/stockanalyzer/entity/Industry.java
package com.stockanalyzer.entity;

import javax.persistence.*;

@Entity
@Table(name = "industries")
public class Industry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    public Industry() {}

    public Industry(String name, String description, Sector sector) {
        this.name = name;
        this.description = description;
        this.sector = sector;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Sector getSector() { return sector; }
    public void setSector(Sector sector) { this.sector = sector; }
}
```

- [ ] **Step 3: Repository 작성**

```java
// backend/src/main/java/com/stockanalyzer/repository/SectorRepository.java
package com.stockanalyzer.repository;

import com.stockanalyzer.entity.Sector;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class SectorRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Sector> findAll() {
        return em.createQuery("SELECT s FROM Sector s", Sector.class).getResultList();
    }

    public Sector findById(Long id) {
        return em.find(Sector.class, id);
    }

    @Transactional
    public Sector save(Sector sector) {
        if (sector.getId() == null) {
            em.persist(sector);
            return sector;
        }
        return em.merge(sector);
    }
}
```

```java
// backend/src/main/java/com/stockanalyzer/repository/IndustryRepository.java
package com.stockanalyzer.repository;

import com.stockanalyzer.entity.Industry;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class IndustryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Industry> findBySectorId(Long sectorId) {
        return em.createQuery(
                "SELECT i FROM Industry i WHERE i.sector.id = :sectorId", Industry.class)
                .setParameter("sectorId", sectorId)
                .getResultList();
    }

    public Industry findById(Long id) {
        return em.find(Industry.class, id);
    }

    @Transactional
    public Industry save(Industry industry) {
        if (industry.getId() == null) {
            em.persist(industry);
            return industry;
        }
        return em.merge(industry);
    }
}
```

- [ ] **Step 4: GICS 섹터/산업 초기 데이터 SQL 작성**

```sql
-- backend/src/main/resources/data/sectors-industries.sql
-- 11 GICS Sectors + major industries

INSERT INTO sectors (name, description) VALUES
('Technology', 'Information Technology'),
('Healthcare', 'Healthcare'),
('Financials', 'Financials'),
('Consumer Discretionary', 'Consumer Discretionary'),
('Consumer Staples', 'Consumer Staples'),
('Energy', 'Energy'),
('Industrials', 'Industrials'),
('Materials', 'Materials'),
('Real Estate', 'Real Estate'),
('Utilities', 'Utilities'),
('Communication Services', 'Communication Services')
ON CONFLICT (name) DO NOTHING;

-- Technology industries
INSERT INTO industries (name, description, sector_id) VALUES
('Software', 'Software & Services', (SELECT id FROM sectors WHERE name = 'Technology')),
('Semiconductors', 'Semiconductors & Equipment', (SELECT id FROM sectors WHERE name = 'Technology')),
('Hardware', 'Technology Hardware & Equipment', (SELECT id FROM sectors WHERE name = 'Technology'));

-- Healthcare industries
INSERT INTO industries (name, description, sector_id) VALUES
('Pharmaceuticals', 'Pharmaceuticals', (SELECT id FROM sectors WHERE name = 'Healthcare')),
('Biotechnology', 'Biotechnology', (SELECT id FROM sectors WHERE name = 'Healthcare')),
('Medical Devices', 'Healthcare Equipment & Supplies', (SELECT id FROM sectors WHERE name = 'Healthcare'));

-- Financials industries
INSERT INTO industries (name, description, sector_id) VALUES
('Banks', 'Banks', (SELECT id FROM sectors WHERE name = 'Financials')),
('Insurance', 'Insurance', (SELECT id FROM sectors WHERE name = 'Financials')),
('Capital Markets', 'Capital Markets', (SELECT id FROM sectors WHERE name = 'Financials'));

-- Energy industries
INSERT INTO industries (name, description, sector_id) VALUES
('Oil & Gas', 'Oil, Gas & Consumable Fuels', (SELECT id FROM sectors WHERE name = 'Energy')),
('Energy Equipment', 'Energy Equipment & Services', (SELECT id FROM sectors WHERE name = 'Energy'));

-- Consumer Discretionary industries
INSERT INTO industries (name, description, sector_id) VALUES
('Retail', 'Retail', (SELECT id FROM sectors WHERE name = 'Consumer Discretionary')),
('Automobiles', 'Automobiles & Components', (SELECT id FROM sectors WHERE name = 'Consumer Discretionary'));

-- Consumer Staples industries
INSERT INTO industries (name, description, sector_id) VALUES
('Food & Beverage', 'Food, Beverage & Tobacco', (SELECT id FROM sectors WHERE name = 'Consumer Staples')),
('Household Products', 'Household & Personal Products', (SELECT id FROM sectors WHERE name = 'Consumer Staples'));

-- Industrials industries
INSERT INTO industries (name, description, sector_id) VALUES
('Aerospace & Defense', 'Aerospace & Defense', (SELECT id FROM sectors WHERE name = 'Industrials')),
('Transportation', 'Transportation', (SELECT id FROM sectors WHERE name = 'Industrials'));

-- Materials industries
INSERT INTO industries (name, description, sector_id) VALUES
('Chemicals', 'Chemicals', (SELECT id FROM sectors WHERE name = 'Materials')),
('Metals & Mining', 'Metals & Mining', (SELECT id FROM sectors WHERE name = 'Materials'));

-- Real Estate industries
INSERT INTO industries (name, description, sector_id) VALUES
('REITs', 'Equity Real Estate Investment Trusts', (SELECT id FROM sectors WHERE name = 'Real Estate'));

-- Utilities industries
INSERT INTO industries (name, description, sector_id) VALUES
('Electric Utilities', 'Electric Utilities', (SELECT id FROM sectors WHERE name = 'Utilities'));

-- Communication Services industries
INSERT INTO industries (name, description, sector_id) VALUES
('Media', 'Media & Entertainment', (SELECT id FROM sectors WHERE name = 'Communication Services')),
('Telecom', 'Telecommunication Services', (SELECT id FROM sectors WHERE name = 'Communication Services'));
```

- [ ] **Step 5: 빌드 확인**

Run: `cd backend && gradle build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/stockanalyzer/entity/Sector.java \
        backend/src/main/java/com/stockanalyzer/entity/Industry.java \
        backend/src/main/java/com/stockanalyzer/repository/SectorRepository.java \
        backend/src/main/java/com/stockanalyzer/repository/IndustryRepository.java \
        backend/src/main/resources/data/sectors-industries.sql
git commit -m "feat: add Sector, Industry entities with repositories and seed data"
```

---

## Task 3: Entity + Repository — Stock

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/entity/Stock.java`
- Create: `backend/src/main/java/com/stockanalyzer/repository/StockRepository.java`
- Test: `backend/src/test/java/com/stockanalyzer/repository/StockRepositoryTest.java`

- [ ] **Step 1: 테스트 작성 — Stock 검색**

```java
// backend/src/test/java/com/stockanalyzer/repository/StockRepositoryTest.java
package com.stockanalyzer.repository;

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
public class StockRepositoryTest {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private IndustryRepository industryRepository;

    private Stock apple;

    @Before
    public void setUp() {
        Sector tech = sectorRepository.save(new Sector("Technology", "Tech sector"));
        Industry software = industryRepository.save(new Industry("Software", "Software", tech));

        apple = new Stock();
        apple.setTicker("AAPL");
        apple.setCompanyName("Apple Inc.");
        apple.setExchange("NASDAQ");
        apple.setMarketCap(new BigDecimal("3000000000000"));
        apple.setIndustry(software);
        apple = stockRepository.save(apple);

        Stock msft = new Stock();
        msft.setTicker("MSFT");
        msft.setCompanyName("Microsoft Corporation");
        msft.setExchange("NASDAQ");
        msft.setMarketCap(new BigDecimal("2800000000000"));
        msft.setIndustry(software);
        stockRepository.save(msft);
    }

    @Test
    public void searchByTicker_returnsMatch() {
        List<Stock> results = stockRepository.search("AAPL", 10);
        assertEquals(1, results.size());
        assertEquals("AAPL", results.get(0).getTicker());
    }

    @Test
    public void searchByCompanyName_returnsMatch() {
        List<Stock> results = stockRepository.search("Apple", 10);
        assertEquals(1, results.size());
        assertEquals("Apple Inc.", results.get(0).getCompanyName());
    }

    @Test
    public void searchPartial_returnsBothMatches() {
        List<Stock> results = stockRepository.search("M", 10);
        assertEquals(1, results.size()); // only MSFT matches
    }

    @Test
    public void findByTicker_returnsStock() {
        Stock found = stockRepository.findByTicker("AAPL");
        assertNotNull(found);
        assertEquals("Apple Inc.", found.getCompanyName());
    }

    @Test
    public void findByTicker_notFound_returnsNull() {
        Stock found = stockRepository.findByTicker("ZZZZ");
        assertNull(found);
    }
}
```

- [ ] **Step 2: TestConfig 작성 (H2 인메모리 DB)**

```java
// backend/src/test/java/com/stockanalyzer/repository/TestConfig.java
package com.stockanalyzer.repository;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = "com.stockanalyzer")
@EnableTransactionManagement
public class TestConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.stockanalyzer.entity");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties props = new Properties();
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        props.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        props.setProperty("hibernate.show_sql", "true");
        em.setJpaProperties(props);

        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
```

- [ ] **Step 3: 테스트 실행 — 실패 확인**

Run: `cd backend && gradle test --tests '*StockRepositoryTest'`
Expected: FAIL — Stock 클래스가 존재하지 않음

- [ ] **Step 4: Stock Entity 작성**

```java
// backend/src/main/java/com/stockanalyzer/entity/Stock.java
package com.stockanalyzer.entity;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String ticker;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(length = 20)
    private String exchange;

    @Column(name = "market_cap", precision = 20, scale = 2)
    private BigDecimal marketCap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_id")
    private Industry industry;

    public Stock() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
    public Industry getIndustry() { return industry; }
    public void setIndustry(Industry industry) { this.industry = industry; }
}
```

- [ ] **Step 5: StockRepository 작성**

```java
// backend/src/main/java/com/stockanalyzer/repository/StockRepository.java
package com.stockanalyzer.repository;

import com.stockanalyzer.entity.Stock;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class StockRepository {

    @PersistenceContext
    private EntityManager em;

    public Stock findByTicker(String ticker) {
        List<Stock> results = em.createQuery(
                "SELECT s FROM Stock s WHERE s.ticker = :ticker", Stock.class)
                .setParameter("ticker", ticker.toUpperCase())
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public List<Stock> search(String keyword, int limit) {
        String pattern = keyword.toUpperCase() + "%";
        return em.createQuery(
                "SELECT s FROM Stock s WHERE UPPER(s.ticker) LIKE :pattern " +
                "OR UPPER(s.companyName) LIKE :pattern ORDER BY s.ticker", Stock.class)
                .setParameter("pattern", pattern)
                .setMaxResults(limit)
                .getResultList();
    }

    @Transactional
    public Stock save(Stock stock) {
        if (stock.getId() == null) {
            em.persist(stock);
            return stock;
        }
        return em.merge(stock);
    }
}
```

- [ ] **Step 6: 테스트 실행 — 성공 확인**

Run: `cd backend && gradle test --tests '*StockRepositoryTest'`
Expected: All 5 tests PASS

- [ ] **Step 7: 커밋**

```bash
git add backend/src/main/java/com/stockanalyzer/entity/Stock.java \
        backend/src/main/java/com/stockanalyzer/repository/StockRepository.java \
        backend/src/test/
git commit -m "feat: add Stock entity with search repository and tests"
```

---

## Task 4: Entity + Repository — DailyPrice

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/entity/DailyPrice.java`
- Create: `backend/src/main/java/com/stockanalyzer/repository/DailyPriceRepository.java`
- Test: `backend/src/test/java/com/stockanalyzer/repository/DailyPriceRepositoryTest.java`

- [ ] **Step 1: 테스트 작성**

```java
// backend/src/test/java/com/stockanalyzer/repository/DailyPriceRepositoryTest.java
package com.stockanalyzer.repository;

import com.stockanalyzer.entity.DailyPrice;
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
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class DailyPriceRepositoryTest {

    @Autowired
    private DailyPriceRepository dailyPriceRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private IndustryRepository industryRepository;

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

        for (int i = 0; i < 5; i++) {
            DailyPrice dp = new DailyPrice();
            dp.setStock(apple);
            dp.setDate(LocalDate.of(2026, 3, 20 + i));
            dp.setOpen(new BigDecimal("170.00").add(new BigDecimal(i)));
            dp.setHigh(new BigDecimal("175.00").add(new BigDecimal(i)));
            dp.setLow(new BigDecimal("168.00").add(new BigDecimal(i)));
            dp.setClose(new BigDecimal("173.00").add(new BigDecimal(i)));
            dp.setAdjustedClose(new BigDecimal("173.00").add(new BigDecimal(i)));
            dp.setVolume(50000000L + i * 1000000L);
            dailyPriceRepository.save(dp);
        }
    }

    @Test
    public void findByStockAndDateRange_returnsCorrectPrices() {
        List<DailyPrice> prices = dailyPriceRepository.findByStockAndDateRange(
                apple.getId(), LocalDate.of(2026, 3, 21), LocalDate.of(2026, 3, 23));
        assertEquals(3, prices.size());
    }

    @Test
    public void findLatestByStock_returnsNewestDate() {
        DailyPrice latest = dailyPriceRepository.findLatestByStock(apple.getId());
        assertNotNull(latest);
        assertEquals(LocalDate.of(2026, 3, 24), latest.getDate());
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

Run: `cd backend && gradle test --tests '*DailyPriceRepositoryTest'`
Expected: FAIL — DailyPrice 클래스가 존재하지 않음

- [ ] **Step 3: DailyPrice Entity 작성**

```java
// backend/src/main/java/com/stockanalyzer/entity/DailyPrice.java
package com.stockanalyzer.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_prices",
       uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "date"}),
       indexes = @Index(columnList = "stock_id, date"))
public class DailyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private LocalDate date;

    @Column(precision = 12, scale = 4)
    private BigDecimal open;

    @Column(precision = 12, scale = 4)
    private BigDecimal high;

    @Column(precision = 12, scale = 4)
    private BigDecimal low;

    @Column(name = "close_price", precision = 12, scale = 4)
    private BigDecimal close;

    @Column(name = "adjusted_close", precision = 12, scale = 4)
    private BigDecimal adjustedClose;

    private Long volume;

    public DailyPrice() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }
    public BigDecimal getAdjustedClose() { return adjustedClose; }
    public void setAdjustedClose(BigDecimal adjustedClose) { this.adjustedClose = adjustedClose; }
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
}
```

- [ ] **Step 4: DailyPriceRepository 작성**

```java
// backend/src/main/java/com/stockanalyzer/repository/DailyPriceRepository.java
package com.stockanalyzer.repository;

import com.stockanalyzer.entity.DailyPrice;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class DailyPriceRepository {

    @PersistenceContext
    private EntityManager em;

    public List<DailyPrice> findByStockAndDateRange(Long stockId, LocalDate from, LocalDate to) {
        return em.createQuery(
                "SELECT dp FROM DailyPrice dp WHERE dp.stock.id = :stockId " +
                "AND dp.date >= :from AND dp.date <= :to ORDER BY dp.date ASC", DailyPrice.class)
                .setParameter("stockId", stockId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public DailyPrice findLatestByStock(Long stockId) {
        List<DailyPrice> results = em.createQuery(
                "SELECT dp FROM DailyPrice dp WHERE dp.stock.id = :stockId " +
                "ORDER BY dp.date DESC", DailyPrice.class)
                .setParameter("stockId", stockId)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Transactional
    public DailyPrice save(DailyPrice price) {
        if (price.getId() == null) {
            em.persist(price);
            return price;
        }
        return em.merge(price);
    }

    @Transactional
    public void saveAll(List<DailyPrice> prices) {
        for (int i = 0; i < prices.size(); i++) {
            save(prices.get(i));
            if (i % 50 == 0) {
                em.flush();
                em.clear();
            }
        }
    }
}
```

- [ ] **Step 5: 테스트 실행 — 성공 확인**

Run: `cd backend && gradle test --tests '*DailyPriceRepositoryTest'`
Expected: All 2 tests PASS

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/stockanalyzer/entity/DailyPrice.java \
        backend/src/main/java/com/stockanalyzer/repository/DailyPriceRepository.java \
        backend/src/test/java/com/stockanalyzer/repository/DailyPriceRepositoryTest.java
git commit -m "feat: add DailyPrice entity with date range query and batch save"
```

---

## Task 5: Yahoo Finance Client

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/client/YahooFinanceClient.java`
- Test: `backend/src/test/java/com/stockanalyzer/client/YahooFinanceClientTest.java`

- [ ] **Step 1: 테스트 작성 — JSON 파싱**

```java
// backend/src/test/java/com/stockanalyzer/client/YahooFinanceClientTest.java
package com.stockanalyzer.client;

import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

public class YahooFinanceClientTest {

    @Test
    public void parseQuoteResponse_extractsStockInfo() {
        String json = "{\"quoteResponse\":{\"result\":[{" +
                "\"symbol\":\"AAPL\"," +
                "\"shortName\":\"Apple Inc.\"," +
                "\"fullExchangeName\":\"NASDAQ\"," +
                "\"marketCap\":3000000000000," +
                "\"trailingPE\":28.5," +
                "\"regularMarketPrice\":173.50" +
                "}]}}";

        StockDetail detail = YahooFinanceClient.parseQuoteResponse(json);
        assertEquals("AAPL", detail.getTicker());
        assertEquals("Apple Inc.", detail.getCompanyName());
        assertEquals(new BigDecimal("173.50"), detail.getCurrentPrice());
    }

    @Test
    public void parseChartResponse_extractsPriceHistory() {
        String json = "{\"chart\":{\"result\":[{" +
                "\"timestamp\":[1711929600,1712016000]," +
                "\"indicators\":{\"quote\":[{" +
                "\"open\":[170.0,171.5]," +
                "\"high\":[175.0,176.0]," +
                "\"low\":[168.0,169.5]," +
                "\"close\":[173.0,174.5]," +
                "\"volume\":[50000000,48000000]" +
                "}],\"adjclose\":[{\"adjclose\":[173.0,174.5]}]}}]}}";

        List<PriceData> prices = YahooFinanceClient.parseChartResponse(json);
        assertEquals(2, prices.size());
        assertEquals(new BigDecimal("170.0"), prices.get(0).getOpen());
        assertEquals(new BigDecimal("174.5"), prices.get(1).getClose());
        assertEquals(50000000L, prices.get(0).getVolume());
    }
}
```

- [ ] **Step 2: DTO 작성**

```java
// backend/src/main/java/com/stockanalyzer/dto/StockDetail.java
package com.stockanalyzer.dto;

import java.math.BigDecimal;

public class StockDetail {
    private String ticker;
    private String companyName;
    private String exchange;
    private BigDecimal marketCap;
    private BigDecimal currentPrice;
    private BigDecimal trailingPE;
    private String sectorName;
    private String industryName;

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public BigDecimal getTrailingPE() { return trailingPE; }
    public void setTrailingPE(BigDecimal trailingPE) { this.trailingPE = trailingPE; }
    public String getSectorName() { return sectorName; }
    public void setSectorName(String sectorName) { this.sectorName = sectorName; }
    public String getIndustryName() { return industryName; }
    public void setIndustryName(String industryName) { this.industryName = industryName; }
}
```

```java
// backend/src/main/java/com/stockanalyzer/dto/PriceData.java
package com.stockanalyzer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceData {
    private LocalDate date;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal adjustedClose;
    private long volume;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }
    public BigDecimal getAdjustedClose() { return adjustedClose; }
    public void setAdjustedClose(BigDecimal adjustedClose) { this.adjustedClose = adjustedClose; }
    public long getVolume() { return volume; }
    public void setVolume(long volume) { this.volume = volume; }
}
```

- [ ] **Step 3: 테스트 실행 — 실패 확인**

Run: `cd backend && gradle test --tests '*YahooFinanceClientTest'`
Expected: FAIL — YahooFinanceClient 클래스가 존재하지 않음

- [ ] **Step 4: YahooFinanceClient 작성**

```java
// backend/src/main/java/com/stockanalyzer/client/YahooFinanceClient.java
package com.stockanalyzer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class YahooFinanceClient {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${yahoo.finance.base-url:https://query1.finance.yahoo.com}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 종목 시세 정보를 가져온다 (quote endpoint).
     */
    public StockDetail fetchQuote(String ticker) {
        String url = baseUrl + "/v7/finance/quote?symbols=" + ticker;
        String json = restTemplate.getForObject(url, String.class);
        return parseQuoteResponse(json);
    }

    /**
     * 종목 가격 이력을 가져온다 (chart endpoint).
     * @param range 기간: 1mo, 3mo, 1y, 5y
     * @param interval 간격: 1d, 1wk, 1mo
     */
    public List<PriceData> fetchPriceHistory(String ticker, String range, String interval) {
        String url = baseUrl + "/v8/finance/chart/" + ticker +
                "?range=" + range + "&interval=" + interval;
        String json = restTemplate.getForObject(url, String.class);
        return parseChartResponse(json);
    }

    static StockDetail parseQuoteResponse(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode result = root.path("quoteResponse").path("result").get(0);

            StockDetail detail = new StockDetail();
            detail.setTicker(result.path("symbol").asText());
            detail.setCompanyName(result.path("shortName").asText());
            detail.setExchange(result.path("fullExchangeName").asText());
            detail.setMarketCap(new BigDecimal(result.path("marketCap").asText()));
            detail.setCurrentPrice(new BigDecimal(result.path("regularMarketPrice").asText()));
            if (result.has("trailingPE") && !result.path("trailingPE").isNull()) {
                detail.setTrailingPE(new BigDecimal(result.path("trailingPE").asText()));
            }
            return detail;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Yahoo Finance quote response", e);
        }
    }

    static List<PriceData> parseChartResponse(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode result = root.path("chart").path("result").get(0);
            JsonNode timestamps = result.path("timestamp");
            JsonNode quote = result.path("indicators").path("quote").get(0);
            JsonNode adjClose = result.path("indicators").path("adjclose").get(0).path("adjclose");

            List<PriceData> prices = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i++) {
                long ts = timestamps.get(i).asLong();
                LocalDate date = Instant.ofEpochSecond(ts)
                        .atZone(ZoneId.of("America/New_York")).toLocalDate();

                PriceData pd = new PriceData();
                pd.setDate(date);
                pd.setOpen(nodeToDecimal(quote.path("open").get(i)));
                pd.setHigh(nodeToDecimal(quote.path("high").get(i)));
                pd.setLow(nodeToDecimal(quote.path("low").get(i)));
                pd.setClose(nodeToDecimal(quote.path("close").get(i)));
                pd.setAdjustedClose(nodeToDecimal(adjClose.get(i)));
                pd.setVolume(quote.path("volume").get(i).asLong());
                prices.add(pd);
            }
            return prices;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Yahoo Finance chart response", e);
        }
    }

    private static BigDecimal nodeToDecimal(JsonNode node) {
        if (node == null || node.isNull()) return BigDecimal.ZERO;
        return new BigDecimal(node.asText());
    }
}
```

- [ ] **Step 5: 테스트 실행 — 성공 확인**

Run: `cd backend && gradle test --tests '*YahooFinanceClientTest'`
Expected: All 2 tests PASS

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/stockanalyzer/client/ \
        backend/src/main/java/com/stockanalyzer/dto/ \
        backend/src/test/java/com/stockanalyzer/client/
git commit -m "feat: add Yahoo Finance client with quote and chart data parsing"
```

---

## Task 6: StockService 비즈니스 로직

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/service/StockService.java`
- Create: `backend/src/main/java/com/stockanalyzer/dto/StockSearchResult.java`
- Test: `backend/src/test/java/com/stockanalyzer/service/StockServiceTest.java`

- [ ] **Step 1: 테스트 작성**

```java
// backend/src/test/java/com/stockanalyzer/service/StockServiceTest.java
package com.stockanalyzer.service;

import com.stockanalyzer.client.YahooFinanceClient;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import com.stockanalyzer.dto.StockSearchResult;
import com.stockanalyzer.entity.DailyPrice;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.repository.DailyPriceRepository;
import com.stockanalyzer.repository.StockRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private DailyPriceRepository dailyPriceRepository;

    @Mock
    private YahooFinanceClient yahooClient;

    @InjectMocks
    private StockService stockService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void search_returnsStockSearchResults() {
        Stock stock = new Stock();
        stock.setTicker("AAPL");
        stock.setCompanyName("Apple Inc.");
        when(stockRepository.search("AAPL", 10)).thenReturn(Arrays.asList(stock));

        List<StockSearchResult> results = stockService.search("AAPL");
        assertEquals(1, results.size());
        assertEquals("AAPL", results.get(0).getTicker());
        assertEquals("Apple Inc.", results.get(0).getCompanyName());
    }

    @Test
    public void getDetail_whenStockExistsInDB_enrichesWithYahooData() {
        Stock stock = new Stock();
        stock.setTicker("AAPL");
        stock.setCompanyName("Apple Inc.");
        when(stockRepository.findByTicker("AAPL")).thenReturn(stock);

        StockDetail yahooDetail = new StockDetail();
        yahooDetail.setCurrentPrice(new BigDecimal("175.00"));
        yahooDetail.setTrailingPE(new BigDecimal("28.5"));
        when(yahooClient.fetchQuote("AAPL")).thenReturn(yahooDetail);

        StockDetail result = stockService.getDetail("AAPL");
        assertEquals("AAPL", result.getTicker());
        assertEquals(new BigDecimal("175.00"), result.getCurrentPrice());
    }

    @Test
    public void getPrices_returnsFromDB() {
        DailyPrice dp = new DailyPrice();
        dp.setDate(LocalDate.of(2026, 3, 20));
        dp.setOpen(new BigDecimal("170.00"));
        dp.setClose(new BigDecimal("173.00"));
        dp.setHigh(new BigDecimal("175.00"));
        dp.setLow(new BigDecimal("168.00"));
        dp.setVolume(50000000L);
        dp.setAdjustedClose(new BigDecimal("173.00"));

        Stock stock = new Stock();
        stock.setId(1L);
        stock.setTicker("AAPL");
        when(stockRepository.findByTicker("AAPL")).thenReturn(stock);
        when(dailyPriceRepository.findByStockAndDateRange(eq(1L), any(), any()))
                .thenReturn(Arrays.asList(dp));

        List<PriceData> prices = stockService.getPrices("AAPL", "1m");
        assertEquals(1, prices.size());
        assertEquals(new BigDecimal("173.00"), prices.get(0).getClose());
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

Run: `cd backend && gradle test --tests '*StockServiceTest'`
Expected: FAIL — StockService, StockSearchResult 클래스가 존재하지 않음

- [ ] **Step 3: StockSearchResult DTO 작성**

```java
// backend/src/main/java/com/stockanalyzer/dto/StockSearchResult.java
package com.stockanalyzer.dto;

public class StockSearchResult {
    private String ticker;
    private String companyName;
    private String exchange;

    public StockSearchResult(String ticker, String companyName, String exchange) {
        this.ticker = ticker;
        this.companyName = companyName;
        this.exchange = exchange;
    }

    public String getTicker() { return ticker; }
    public String getCompanyName() { return companyName; }
    public String getExchange() { return exchange; }
}
```

- [ ] **Step 4: StockService 작성**

```java
// backend/src/main/java/com/stockanalyzer/service/StockService.java
package com.stockanalyzer.service;

import com.stockanalyzer.client.YahooFinanceClient;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import com.stockanalyzer.dto.StockSearchResult;
import com.stockanalyzer.entity.DailyPrice;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.repository.DailyPriceRepository;
import com.stockanalyzer.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final YahooFinanceClient yahooClient;

    @Autowired
    public StockService(StockRepository stockRepository,
                        DailyPriceRepository dailyPriceRepository,
                        YahooFinanceClient yahooClient) {
        this.stockRepository = stockRepository;
        this.dailyPriceRepository = dailyPriceRepository;
        this.yahooClient = yahooClient;
    }

    public List<StockSearchResult> search(String keyword) {
        return stockRepository.search(keyword, 10).stream()
                .map(s -> new StockSearchResult(s.getTicker(), s.getCompanyName(), s.getExchange()))
                .collect(Collectors.toList());
    }

    public StockDetail getDetail(String ticker) {
        Stock stock = stockRepository.findByTicker(ticker.toUpperCase());
        if (stock == null) {
            return yahooClient.fetchQuote(ticker);
        }

        StockDetail detail = yahooClient.fetchQuote(ticker);
        detail.setTicker(stock.getTicker());
        detail.setCompanyName(stock.getCompanyName());
        detail.setExchange(stock.getExchange());
        if (stock.getIndustry() != null) {
            detail.setIndustryName(stock.getIndustry().getName());
            detail.setSectorName(stock.getIndustry().getSector().getName());
        }
        return detail;
    }

    public List<PriceData> getPrices(String ticker, String period) {
        Stock stock = stockRepository.findByTicker(ticker.toUpperCase());
        if (stock == null) {
            return yahooClient.fetchPriceHistory(ticker, periodToRange(period), "1d");
        }

        LocalDate to = LocalDate.now();
        LocalDate from = periodToFromDate(period, to);

        return dailyPriceRepository.findByStockAndDateRange(stock.getId(), from, to)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private PriceData toDto(DailyPrice dp) {
        PriceData pd = new PriceData();
        pd.setDate(dp.getDate());
        pd.setOpen(dp.getOpen());
        pd.setHigh(dp.getHigh());
        pd.setLow(dp.getLow());
        pd.setClose(dp.getClose());
        pd.setAdjustedClose(dp.getAdjustedClose());
        pd.setVolume(dp.getVolume());
        return pd;
    }

    private LocalDate periodToFromDate(String period, LocalDate to) {
        switch (period) {
            case "1m": return to.minusMonths(1);
            case "3m": return to.minusMonths(3);
            case "1y": return to.minusYears(1);
            case "5y": return to.minusYears(5);
            default: return to.minusMonths(1);
        }
    }

    private String periodToRange(String period) {
        switch (period) {
            case "1m": return "1mo";
            case "3m": return "3mo";
            case "1y": return "1y";
            case "5y": return "5y";
            default: return "1mo";
        }
    }
}
```

- [ ] **Step 5: 테스트 실행 — 성공 확인**

Run: `cd backend && gradle test --tests '*StockServiceTest'`
Expected: All 3 tests PASS

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/stockanalyzer/service/StockService.java \
        backend/src/main/java/com/stockanalyzer/dto/StockSearchResult.java \
        backend/src/test/java/com/stockanalyzer/service/StockServiceTest.java
git commit -m "feat: add StockService with search, detail, and price retrieval"
```

---

## Task 7: REST Controller + ApiResponse

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/common/ApiResponse.java`
- Create: `backend/src/main/java/com/stockanalyzer/controller/StockController.java`
- Test: `backend/src/test/java/com/stockanalyzer/controller/StockControllerTest.java`

- [ ] **Step 1: ApiResponse wrapper 작성**

```java
// backend/src/main/java/com/stockanalyzer/common/ApiResponse.java
package com.stockanalyzer.common;

import java.util.Map;

public class ApiResponse<T> {
    private boolean success;
    private T data;
    private Map<String, Object> meta;

    private ApiResponse(boolean success, T data, Map<String, Object> meta) {
        this.success = success;
        this.data = data;
        this.meta = meta;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok(T data, Map<String, Object> meta) {
        return new ApiResponse<>(true, data, meta);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, Map.of("error", message));
    }

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public Map<String, Object> getMeta() { return meta; }
}
```

- [ ] **Step 2: 컨트롤러 테스트 작성**

```java
// backend/src/test/java/com/stockanalyzer/controller/StockControllerTest.java
package com.stockanalyzer.controller;

import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import com.stockanalyzer.dto.StockSearchResult;
import com.stockanalyzer.service.StockService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class StockControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StockService stockService;

    @InjectMocks
    private StockController stockController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(stockController).build();
    }

    @Test
    public void searchStocks_returnsResults() throws Exception {
        when(stockService.search("AAPL")).thenReturn(
                Arrays.asList(new StockSearchResult("AAPL", "Apple Inc.", "NASDAQ")));

        mockMvc.perform(get("/api/stocks").param("q", "AAPL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].ticker").value("AAPL"));
    }

    @Test
    public void getStockDetail_returnsDetail() throws Exception {
        StockDetail detail = new StockDetail();
        detail.setTicker("AAPL");
        detail.setCompanyName("Apple Inc.");
        detail.setCurrentPrice(new BigDecimal("175.00"));
        when(stockService.getDetail("AAPL")).thenReturn(detail);

        mockMvc.perform(get("/api/stocks/AAPL").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.currentPrice").value(175.00));
    }

    @Test
    public void getStockPrices_returnsPriceList() throws Exception {
        PriceData pd = new PriceData();
        pd.setDate(LocalDate.of(2026, 3, 20));
        pd.setClose(new BigDecimal("173.00"));
        pd.setOpen(new BigDecimal("170.00"));
        pd.setHigh(new BigDecimal("175.00"));
        pd.setLow(new BigDecimal("168.00"));
        pd.setVolume(50000000L);
        when(stockService.getPrices("AAPL", "1m")).thenReturn(Arrays.asList(pd));

        mockMvc.perform(get("/api/stocks/AAPL/prices").param("period", "1m")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].close").value(173.00));
    }
}
```

- [ ] **Step 3: 테스트 실행 — 실패 확인**

Run: `cd backend && gradle test --tests '*StockControllerTest'`
Expected: FAIL — StockController 클래스가 존재하지 않음

- [ ] **Step 4: StockController 작성**

```java
// backend/src/main/java/com/stockanalyzer/controller/StockController.java
package com.stockanalyzer.controller;

import com.stockanalyzer.common.ApiResponse;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import com.stockanalyzer.dto.StockSearchResult;
import com.stockanalyzer.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    @Autowired
    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public ApiResponse<List<StockSearchResult>> search(@RequestParam("q") String keyword) {
        return ApiResponse.ok(stockService.search(keyword));
    }

    @GetMapping("/{ticker}")
    public ApiResponse<StockDetail> getDetail(@PathVariable String ticker) {
        StockDetail detail = stockService.getDetail(ticker);
        if (detail == null) {
            return ApiResponse.error("Stock not found: " + ticker);
        }
        return ApiResponse.ok(detail);
    }

    @GetMapping("/{ticker}/prices")
    public ApiResponse<List<PriceData>> getPrices(
            @PathVariable String ticker,
            @RequestParam(value = "period", defaultValue = "1m") String period) {
        return ApiResponse.ok(stockService.getPrices(ticker, period));
    }
}
```

- [ ] **Step 5: 테스트 실행 — 성공 확인**

Run: `cd backend && gradle test --tests '*StockControllerTest'`
Expected: All 3 tests PASS

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/stockanalyzer/common/ApiResponse.java \
        backend/src/main/java/com/stockanalyzer/controller/StockController.java \
        backend/src/test/java/com/stockanalyzer/controller/StockControllerTest.java
git commit -m "feat: add StockController REST API with search, detail, and prices"
```

---

## Task 8: 데이터 수집 스케줄러

**Files:**
- Create: `backend/src/main/java/com/stockanalyzer/scheduler/PriceCollector.java`

- [ ] **Step 1: PriceCollector 작성**

```java
// backend/src/main/java/com/stockanalyzer/scheduler/PriceCollector.java
package com.stockanalyzer.scheduler;

import com.stockanalyzer.client.YahooFinanceClient;
import com.stockanalyzer.dto.PriceData;
import com.stockanalyzer.dto.StockDetail;
import com.stockanalyzer.entity.DailyPrice;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.repository.DailyPriceRepository;
import com.stockanalyzer.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PriceCollector {

    private static final Logger log = LoggerFactory.getLogger(PriceCollector.class);

    // S&P 500 주요 종목 (초기 수집 대상 — 추후 전체 리스트로 확장)
    private static final List<String> INITIAL_TICKERS = Arrays.asList(
            "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "TSLA", "BRK-B",
            "JPM", "JNJ", "V", "UNH", "HD", "PG", "MA", "XOM", "BAC", "ABBV",
            "KO", "PFE", "MRK", "PEP", "COST", "TMO", "AVGO", "CSCO", "ACN",
            "MCD", "ABT", "WMT", "NKE", "DIS", "ADBE", "CRM", "NFLX", "AMD",
            "INTC", "QCOM", "TXN", "ORCL"
    );

    private final YahooFinanceClient yahooClient;
    private final StockRepository stockRepository;
    private final DailyPriceRepository dailyPriceRepository;

    @Autowired
    public PriceCollector(YahooFinanceClient yahooClient,
                          StockRepository stockRepository,
                          DailyPriceRepository dailyPriceRepository) {
        this.yahooClient = yahooClient;
        this.stockRepository = stockRepository;
        this.dailyPriceRepository = dailyPriceRepository;
    }

    /**
     * 평일 미국 장 마감 후 실행 (EST 17:00 = KST 07:00 다음날)
     * 크론: 매주 화~토 07:00 KST (= 월~금 장 마감 후)
     */
    @Scheduled(cron = "0 0 7 ? * TUE-SAT")
    public void collectDailyPrices() {
        log.info("Starting daily price collection for {} tickers", INITIAL_TICKERS.size());

        for (String ticker : INITIAL_TICKERS) {
            try {
                collectForTicker(ticker);
                Thread.sleep(1200); // rate limit: ~500/hour ≈ 1 req per 1.2s
            } catch (Exception e) {
                log.error("Failed to collect data for {}: {}", ticker, e.getMessage());
            }
        }

        log.info("Daily price collection completed");
    }

    @Transactional
    public void collectForTicker(String ticker) {
        // 종목 정보 확보 (없으면 생성)
        Stock stock = stockRepository.findByTicker(ticker);
        if (stock == null) {
            StockDetail detail = yahooClient.fetchQuote(ticker);
            stock = new Stock();
            stock.setTicker(detail.getTicker());
            stock.setCompanyName(detail.getCompanyName());
            stock.setExchange(detail.getExchange());
            stock.setMarketCap(detail.getMarketCap());
            stock = stockRepository.save(stock);
        }

        // 최근 1개월 가격 데이터 수집
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
}
```

- [ ] **Step 2: 빌드 확인**

Run: `cd backend && gradle build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add backend/src/main/java/com/stockanalyzer/scheduler/PriceCollector.java
git commit -m "feat: add PriceCollector scheduler for daily Yahoo Finance data collection"
```

---

## Task 9: Docker + docker-compose 설정

**Files:**
- Create: `backend/Dockerfile`
- Create: `docker-compose.yml`

- [ ] **Step 1: Dockerfile 작성**

```dockerfile
# backend/Dockerfile
FROM gradle:7.6-jdk11 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src/ src/
RUN gradle build -x test --no-daemon

FROM tomcat:9.0-jdk11
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/build/libs/stock-analyzer-1.0.0.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
```

- [ ] **Step 2: docker-compose.yml 작성**

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:15
    container_name: stock-db
    environment:
      POSTGRES_DB: stockanalyzer
      POSTGRES_USER: stockuser
      POSTGRES_PASSWORD: stockpass
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./backend/src/main/resources/data/sectors-industries.sql:/docker-entrypoint-initdb.d/01-seed.sql
    restart: unless-stopped

  spring-app:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: stock-api
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/stockanalyzer
      DB_USERNAME: stockuser
      DB_PASSWORD: stockpass
    depends_on:
      - postgres
    deploy:
      resources:
        limits:
          memory: 512M
    restart: unless-stopped

volumes:
  pgdata:
```

- [ ] **Step 3: AppConfig에서 환경변수 지원 추가**

`backend/src/main/java/com/stockanalyzer/config/AppConfig.java`의 `dataSource()` 메서드를 수정하여 환경변수를 우선 사용하도록 한다:

```java
    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName(dbDriver);
        ds.setJdbcUrl(env("DB_URL", dbUrl));
        ds.setUsername(env("DB_USERNAME", dbUsername));
        ds.setPassword(env("DB_PASSWORD", dbPassword));
        ds.setMaximumPoolSize(10);
        return ds;
    }

    private String env(String key, String fallback) {
        String value = System.getenv(key);
        return value != null ? value : fallback;
    }
```

- [ ] **Step 4: .gitignore 작성**

```gitignore
# .gitignore
# Build
backend/build/
frontend/dist/
frontend/node_modules/

# IDE
.idea/
*.iml
.vscode/

# Environment
.env
backend/src/main/resources/application-local.properties

# Superpowers
.superpowers/

# OS
.DS_Store
```

- [ ] **Step 5: 커밋**

```bash
git add backend/Dockerfile docker-compose.yml .gitignore
git add backend/src/main/java/com/stockanalyzer/config/AppConfig.java
git commit -m "feat: add Docker and docker-compose for Synology deployment"
```

---

## Task 10: React 프론트엔드 스캐폴딩

**Files:**
- Create: `frontend/` (Vite + React + TypeScript)
- Create: `frontend/src/api/stockApi.ts`
- Create: `frontend/.env`

- [ ] **Step 1: Vite 프로젝트 생성**

Run:
```bash
cd /Users/tiger_mac/projects/app-05-springtest
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install axios react-router-dom lightweight-charts recharts
npm install -D tailwindcss @tailwindcss/vite
```

- [ ] **Step 2: Tailwind CSS 설정**

```ts
// frontend/vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
```

`frontend/src/index.css`의 맨 위에 추가:
```css
@import "tailwindcss";
```

- [ ] **Step 3: API 클라이언트 작성**

```ts
// frontend/src/api/stockApi.ts
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
});

export interface StockSearchResult {
  ticker: string;
  companyName: string;
  exchange: string;
}

export interface StockDetail {
  ticker: string;
  companyName: string;
  exchange: string;
  marketCap: number;
  currentPrice: number;
  trailingPE: number;
  sectorName: string;
  industryName: string;
}

export interface PriceData {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  adjustedClose: number;
  volume: number;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  meta?: Record<string, unknown>;
}

export async function searchStocks(query: string): Promise<StockSearchResult[]> {
  const res = await api.get<ApiResponse<StockSearchResult[]>>('/stocks', {
    params: { q: query },
  });
  return res.data.data;
}

export async function getStockDetail(ticker: string): Promise<StockDetail> {
  const res = await api.get<ApiResponse<StockDetail>>(`/stocks/${ticker}`);
  return res.data.data;
}

export async function getStockPrices(
  ticker: string,
  period: string = '1m'
): Promise<PriceData[]> {
  const res = await api.get<ApiResponse<PriceData[]>>(`/stocks/${ticker}/prices`, {
    params: { period },
  });
  return res.data.data;
}
```

- [ ] **Step 4: .env 작성**

```env
# frontend/.env
VITE_API_URL=http://localhost:8080/api
```

- [ ] **Step 5: 빌드 확인**

Run: `cd frontend && npm run build`
Expected: 빌드 성공

- [ ] **Step 6: 커밋**

```bash
git add frontend/
git commit -m "feat: scaffold React frontend with Vite, Tailwind, and API client"
```

---

## Task 11: 사이드바 레이아웃 + 종목 검색

**Files:**
- Create: `frontend/src/components/Layout.tsx`
- Create: `frontend/src/components/Sidebar.tsx`
- Create: `frontend/src/components/StockSearch.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: StockSearch 컴포넌트 작성**

```tsx
// frontend/src/components/StockSearch.tsx
import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchStocks, StockSearchResult } from '../api/stockApi';

export default function StockSearch() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<StockSearchResult[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const navigate = useNavigate();
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (query.length < 2) {
      setResults([]);
      setIsOpen(false);
      return;
    }

    const timer = setTimeout(async () => {
      const data = await searchStocks(query);
      setResults(data);
      setIsOpen(data.length > 0);
    }, 300);

    return () => clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  function handleSelect(ticker: string) {
    setQuery('');
    setIsOpen(false);
    navigate(`/stocks/${ticker}`);
  }

  return (
    <div ref={wrapperRef} className="relative">
      <input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="종목 검색..."
        className="w-full bg-slate-700 text-white placeholder-slate-400 px-3 py-2 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
      {isOpen && (
        <ul className="absolute z-50 mt-1 w-full bg-slate-700 rounded-lg shadow-lg max-h-60 overflow-auto">
          {results.map((r) => (
            <li
              key={r.ticker}
              onClick={() => handleSelect(r.ticker)}
              className="px-3 py-2 hover:bg-slate-600 cursor-pointer text-sm"
            >
              <span className="font-bold text-blue-400">{r.ticker}</span>
              <span className="ml-2 text-slate-300">{r.companyName}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Sidebar 컴포넌트 작성**

```tsx
// frontend/src/components/Sidebar.tsx
import { NavLink } from 'react-router-dom';
import StockSearch from './StockSearch';

const navItems = [
  { to: '/', label: '대시보드', icon: '📊' },
  { to: '/screening', label: '스크리닝', icon: '🔍' },
  { to: '/sectors', label: '섹터 분석', icon: '📈' },
  { to: '/compare', label: '종목 비교', icon: '⚖️' },
  { to: '/news', label: '뉴스', icon: '📰' },
];

export default function Sidebar() {
  return (
    <aside className="w-60 bg-slate-800 text-white flex flex-col h-screen fixed left-0 top-0">
      <div className="p-4 border-b border-slate-700">
        <h1 className="text-lg font-bold mb-3">Stock Analyzer</h1>
        <StockSearch />
      </div>
      <nav className="flex-1 p-2">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `flex items-center gap-2 px-3 py-2 rounded-lg text-sm mb-1 ${
                isActive ? 'bg-slate-700 text-white' : 'text-slate-300 hover:bg-slate-700'
              }`
            }
          >
            <span>{item.icon}</span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
```

- [ ] **Step 3: Layout 컴포넌트 작성**

```tsx
// frontend/src/components/Layout.tsx
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';

export default function Layout() {
  return (
    <div className="flex min-h-screen bg-slate-900 text-white">
      <Sidebar />
      <main className="flex-1 ml-60 p-6">
        <Outlet />
      </main>
    </div>
  );
}
```

- [ ] **Step 4: Dashboard 페이지 (플레이스홀더)**

```tsx
// frontend/src/pages/Dashboard.tsx
export default function Dashboard() {
  return (
    <div>
      <h2 className="text-2xl font-bold mb-4">대시보드</h2>
      <p className="text-slate-400">Phase 2에서 구현 예정</p>
    </div>
  );
}
```

- [ ] **Step 5: App.tsx 라우팅 설정**

```tsx
// frontend/src/App.tsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import StockDetail from './pages/StockDetail';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/stocks/:ticker" element={<StockDetail />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
```

- [ ] **Step 6: 빌드 확인**

Run: `cd frontend && npm run build`
Expected: 빌드 성공

- [ ] **Step 7: 커밋**

```bash
git add frontend/src/
git commit -m "feat: add sidebar layout with stock search autocomplete"
```

---

## Task 12: 종목 상세 페이지 + 주가 차트

**Files:**
- Create: `frontend/src/pages/StockDetail.tsx`
- Create: `frontend/src/components/PriceChart.tsx`

- [ ] **Step 1: PriceChart 컴포넌트 작성**

```tsx
// frontend/src/components/PriceChart.tsx
import { useEffect, useRef } from 'react';
import { createChart, IChartApi, CandlestickData, Time } from 'lightweight-charts';
import { PriceData } from '../api/stockApi';

interface Props {
  prices: PriceData[];
}

export default function PriceChart({ prices }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);

  useEffect(() => {
    if (!containerRef.current || prices.length === 0) return;

    if (chartRef.current) {
      chartRef.current.remove();
    }

    const chart = createChart(containerRef.current, {
      width: containerRef.current.clientWidth,
      height: 400,
      layout: {
        background: { color: '#1e293b' },
        textColor: '#94a3b8',
      },
      grid: {
        vertLines: { color: '#334155' },
        horzLines: { color: '#334155' },
      },
      crosshair: { mode: 0 },
    });

    const candleSeries = chart.addCandlestickSeries({
      upColor: '#22c55e',
      downColor: '#ef4444',
      borderUpColor: '#22c55e',
      borderDownColor: '#ef4444',
      wickUpColor: '#22c55e',
      wickDownColor: '#ef4444',
    });

    const data: CandlestickData<Time>[] = prices.map((p) => ({
      time: p.date as unknown as Time,
      open: p.open,
      high: p.high,
      low: p.low,
      close: p.close,
    }));

    candleSeries.setData(data);
    chart.timeScale().fitContent();
    chartRef.current = chart;

    const handleResize = () => {
      if (containerRef.current) {
        chart.applyOptions({ width: containerRef.current.clientWidth });
      }
    };
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      chart.remove();
      chartRef.current = null;
    };
  }, [prices]);

  return <div ref={containerRef} className="w-full rounded-lg overflow-hidden" />;
}
```

- [ ] **Step 2: StockDetail 페이지 작성**

```tsx
// frontend/src/pages/StockDetail.tsx
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getStockDetail, getStockPrices, StockDetail as StockDetailType, PriceData } from '../api/stockApi';
import PriceChart from '../components/PriceChart';

const PERIODS = [
  { label: '1개월', value: '1m' },
  { label: '3개월', value: '3m' },
  { label: '1년', value: '1y' },
  { label: '5년', value: '5y' },
];

export default function StockDetail() {
  const { ticker } = useParams<{ ticker: string }>();
  const [detail, setDetail] = useState<StockDetailType | null>(null);
  const [prices, setPrices] = useState<PriceData[]>([]);
  const [period, setPeriod] = useState('1m');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!ticker) return;
    setLoading(true);
    Promise.all([
      getStockDetail(ticker),
      getStockPrices(ticker, period),
    ]).then(([d, p]) => {
      setDetail(d);
      setPrices(p);
      setLoading(false);
    });
  }, [ticker, period]);

  if (loading) {
    return <div className="text-slate-400">로딩 중...</div>;
  }

  if (!detail) {
    return <div className="text-red-400">종목을 찾을 수 없습니다.</div>;
  }

  function formatMarketCap(cap: number): string {
    if (cap >= 1e12) return `$${(cap / 1e12).toFixed(2)}T`;
    if (cap >= 1e9) return `$${(cap / 1e9).toFixed(2)}B`;
    if (cap >= 1e6) return `$${(cap / 1e6).toFixed(2)}M`;
    return `$${cap}`;
  }

  return (
    <div>
      {/* Header */}
      <div className="mb-6">
        <h2 className="text-2xl font-bold">
          {detail.ticker}
          <span className="ml-3 text-lg text-slate-400 font-normal">{detail.companyName}</span>
        </h2>
        <div className="flex gap-4 mt-2 text-sm text-slate-400">
          <span>{detail.exchange}</span>
          {detail.sectorName && <span>{detail.sectorName}</span>}
          {detail.industryName && <span>{detail.industryName}</span>}
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div className="bg-slate-800 rounded-lg p-4">
          <div className="text-slate-400 text-sm">현재가</div>
          <div className="text-xl font-bold">${detail.currentPrice?.toFixed(2)}</div>
        </div>
        <div className="bg-slate-800 rounded-lg p-4">
          <div className="text-slate-400 text-sm">시가총액</div>
          <div className="text-xl font-bold">{detail.marketCap ? formatMarketCap(detail.marketCap) : '-'}</div>
        </div>
        <div className="bg-slate-800 rounded-lg p-4">
          <div className="text-slate-400 text-sm">PER</div>
          <div className="text-xl font-bold">{detail.trailingPE?.toFixed(2) || '-'}</div>
        </div>
      </div>

      {/* Chart */}
      <div className="bg-slate-800 rounded-lg p-4">
        <div className="flex gap-2 mb-4">
          {PERIODS.map((p) => (
            <button
              key={p.value}
              onClick={() => setPeriod(p.value)}
              className={`px-3 py-1 rounded text-sm ${
                period === p.value
                  ? 'bg-blue-600 text-white'
                  : 'bg-slate-700 text-slate-300 hover:bg-slate-600'
              }`}
            >
              {p.label}
            </button>
          ))}
        </div>
        <PriceChart prices={prices} />
      </div>
    </div>
  );
}
```

- [ ] **Step 3: 빌드 확인**

Run: `cd frontend && npm run build`
Expected: 빌드 성공

- [ ] **Step 4: 커밋**

```bash
git add frontend/src/pages/StockDetail.tsx frontend/src/components/PriceChart.tsx
git commit -m "feat: add stock detail page with candlestick price chart"
```

---

## Task 13: 통합 테스트 및 최종 확인

**Files:** 없음 (기존 파일로 테스트)

- [ ] **Step 1: 백엔드 전체 테스트 실행**

Run: `cd backend && gradle test`
Expected: All tests PASS

- [ ] **Step 2: 프론트엔드 빌드 확인**

Run: `cd frontend && npm run build`
Expected: 빌드 성공, `dist/` 디렉토리 생성

- [ ] **Step 3: Docker 빌드 확인**

Run: `docker-compose build`
Expected: 두 컨테이너 이미지 빌드 성공

- [ ] **Step 4: 최종 커밋**

```bash
git add -A
git commit -m "chore: Phase 1 complete — stock search, detail, chart, data pipeline"
```
