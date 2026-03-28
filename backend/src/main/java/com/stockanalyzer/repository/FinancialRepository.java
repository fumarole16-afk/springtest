package com.stockanalyzer.repository;

import com.stockanalyzer.entity.Financial;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class FinancialRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Financial> findByStockAndType(Long stockId, String type) {
        return em.createQuery(
                "SELECT f FROM Financial f WHERE f.stock.id = :stockId AND f.type = :type ORDER BY f.period DESC",
                Financial.class)
                .setParameter("stockId", stockId)
                .setParameter("type", type)
                .getResultList();
    }

    public Financial findLatestByStock(Long stockId, String type) {
        List<Financial> results = em.createQuery(
                "SELECT f FROM Financial f WHERE f.stock.id = :stockId AND f.type = :type ORDER BY f.period DESC",
                Financial.class)
                .setParameter("stockId", stockId)
                .setParameter("type", type)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public List<Financial> findTwoLatestAnnual(Long stockId) {
        return em.createQuery(
                "SELECT f FROM Financial f WHERE f.stock.id = :stockId AND f.type = 'annual' ORDER BY f.period DESC",
                Financial.class)
                .setParameter("stockId", stockId)
                .setMaxResults(2)
                .getResultList();
    }

    @Transactional
    public Financial save(Financial financial) {
        if (financial.getId() == null) { em.persist(financial); return financial; }
        return em.merge(financial);
    }
}
