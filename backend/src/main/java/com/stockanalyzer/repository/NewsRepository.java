package com.stockanalyzer.repository;

import com.stockanalyzer.entity.News;
import com.stockanalyzer.entity.StockNews;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class NewsRepository {
    @PersistenceContext
    private EntityManager em;

    public List<News> findByStockTicker(String ticker, int page, int size) {
        return em.createQuery(
                "SELECT n FROM News n WHERE n.id IN (" +
                "SELECT sn.news.id FROM StockNews sn WHERE sn.stock.ticker = :ticker) " +
                "ORDER BY n.publishedAt DESC", News.class)
                .setParameter("ticker", ticker.toUpperCase())
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public List<News> findLatest(int page, int size) {
        return em.createQuery(
                "SELECT n FROM News n ORDER BY n.publishedAt DESC", News.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Transactional
    public News save(News news) {
        if (news.getId() == null) { em.persist(news); return news; }
        return em.merge(news);
    }

    @Transactional
    public StockNews saveStockNews(StockNews stockNews) {
        if (stockNews.getId() == null) { em.persist(stockNews); return stockNews; }
        return em.merge(stockNews);
    }
}
