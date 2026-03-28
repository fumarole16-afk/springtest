package com.stockanalyzer.repository;

import com.stockanalyzer.entity.News;
import com.stockanalyzer.entity.Stock;
import com.stockanalyzer.entity.StockNews;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class NewsRepositoryTest {
    @Autowired private NewsRepository newsRepository;
    @Autowired private StockRepository stockRepository;

    private Stock apple;
    private News news1;
    private News news2;

    @Before
    public void setUp() {
        apple = new Stock();
        apple.setTicker("AAPL");
        apple.setCompanyName("Apple Inc.");
        apple = stockRepository.save(apple);

        news1 = new News();
        news1.setTitle("Apple hits new high");
        news1.setSource("Reuters");
        news1.setUrl("https://reuters.com/apple-high");
        news1.setPublishedAt(LocalDateTime.now().minusHours(1));
        news1.setSummary("Apple stock reaches record.");
        news1 = newsRepository.save(news1);

        news2 = new News();
        news2.setTitle("Tech sector rally");
        news2.setSource("Bloomberg");
        news2.setUrl("https://bloomberg.com/tech-rally");
        news2.setPublishedAt(LocalDateTime.now().minusHours(2));
        news2 = newsRepository.save(news2);

        StockNews sn1 = new StockNews();
        sn1.setStock(apple);
        sn1.setNews(news1);
        newsRepository.saveStockNews(sn1);

        StockNews sn2 = new StockNews();
        sn2.setStock(apple);
        sn2.setNews(news2);
        newsRepository.saveStockNews(sn2);
    }

    @Test
    public void findByStockTicker_returnsLinkedNews() {
        List<News> results = newsRepository.findByStockTicker("AAPL", 0, 10);
        assertEquals(2, results.size());
        assertEquals("Apple hits new high", results.get(0).getTitle());
    }

    @Test
    public void findByStockTicker_unknownTicker_returnsEmpty() {
        List<News> results = newsRepository.findByStockTicker("ZZZZ", 0, 10);
        assertTrue(results.isEmpty());
    }

    @Test
    public void findLatest_returnsAllNewsOrderedByDate() {
        List<News> results = newsRepository.findLatest(0, 10);
        assertEquals(2, results.size());
        assertTrue(results.get(0).getPublishedAt().isAfter(results.get(1).getPublishedAt()));
    }

    @Test
    public void findLatest_pagination_works() {
        List<News> page0 = newsRepository.findLatest(0, 1);
        List<News> page1 = newsRepository.findLatest(1, 1);
        assertEquals(1, page0.size());
        assertEquals(1, page1.size());
        assertNotEquals(page0.get(0).getId(), page1.get(0).getId());
    }
}
