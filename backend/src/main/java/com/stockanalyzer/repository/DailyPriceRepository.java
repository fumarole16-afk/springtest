package com.stockanalyzer.repository;

import com.stockanalyzer.entity.DailyPrice;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    public List<DailyPrice> findLatestTwoByStock(Long stockId) {
        return em.createQuery(
                "SELECT dp FROM DailyPrice dp WHERE dp.stock.id = :stockId " +
                "ORDER BY dp.date DESC", DailyPrice.class)
                .setParameter("stockId", stockId)
                .setMaxResults(2)
                .getResultList();
    }

    public LocalDate getLatestDate() {
        List<LocalDate> results = em.createQuery(
                "SELECT MAX(dp.date) FROM DailyPrice dp", LocalDate.class)
                .getResultList();
        return results.isEmpty() || results.get(0) == null ? LocalDate.now() : results.get(0);
    }

    public List<DailyPrice> findByDate(LocalDate date) {
        return em.createQuery(
                "SELECT dp FROM DailyPrice dp JOIN FETCH dp.stock " +
                "WHERE dp.date = :date", DailyPrice.class)
                .setParameter("date", date)
                .getResultList();
    }

    @Transactional
    public DailyPrice save(DailyPrice price) {
        if (price.getId() == null) { em.persist(price); return price; }
        return em.merge(price);
    }

    @Transactional
    public void saveAll(List<DailyPrice> prices) {
        for (int i = 0; i < prices.size(); i++) {
            save(prices.get(i));
            if (i % 50 == 0) { em.flush(); em.clear(); }
        }
    }
}
