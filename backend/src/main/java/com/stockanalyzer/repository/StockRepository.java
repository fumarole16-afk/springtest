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

    public List<Stock> findAll() {
        return em.createQuery("SELECT s FROM Stock s ORDER BY s.ticker", Stock.class).getResultList();
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
        if (stock.getId() == null) { em.persist(stock); return stock; }
        return em.merge(stock);
    }
}
