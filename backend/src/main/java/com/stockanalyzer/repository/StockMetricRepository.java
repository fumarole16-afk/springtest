package com.stockanalyzer.repository;

import com.stockanalyzer.dto.ScreeningFilter;
import com.stockanalyzer.entity.StockMetric;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class StockMetricRepository {

    @PersistenceContext
    private EntityManager em;

    public List<StockMetric> findByFilters(ScreeningFilter filter, int page, int size, String sort) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<StockMetric> cq = cb.createQuery(StockMetric.class);
        Root<StockMetric> root = cq.from(StockMetric.class);
        root.fetch("stock", JoinType.LEFT);

        List<Predicate> predicates = buildPredicates(cb, root, filter);
        cq.where(predicates.toArray(new Predicate[0]));

        applySorting(cb, cq, root, sort);

        return em.createQuery(cq)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public long countByFilters(ScreeningFilter filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<StockMetric> root = cq.from(StockMetric.class);
        cq.select(cb.count(root));

        List<Predicate> predicates = buildPredicates(cb, root, filter);
        cq.where(predicates.toArray(new Predicate[0]));

        return em.createQuery(cq).getSingleResult();
    }

    public LocalDate getLatestDate() {
        List<LocalDate> results = em.createQuery(
                "SELECT MAX(sm.date) FROM StockMetric sm", LocalDate.class)
                .getResultList();
        return results.isEmpty() || results.get(0) == null ? LocalDate.now() : results.get(0);
    }

    public List<StockMetric> findLatestWithAvgVolume() {
        LocalDate latestDate = getLatestDate();
        return em.createQuery(
                "SELECT sm FROM StockMetric sm JOIN FETCH sm.stock " +
                "WHERE sm.date = :date AND sm.avgVolume30d IS NOT NULL AND sm.avgVolume30d > 0",
                StockMetric.class)
                .setParameter("date", latestDate)
                .getResultList();
    }

    public List<StockMetric> findLatestWithExtremes() {
        LocalDate latestDate = getLatestDate();
        return em.createQuery(
                "SELECT sm FROM StockMetric sm JOIN FETCH sm.stock " +
                "WHERE sm.date = :date AND sm.week52High IS NOT NULL AND sm.week52Low IS NOT NULL",
                StockMetric.class)
                .setParameter("date", latestDate)
                .getResultList();
    }

    @Transactional
    public StockMetric save(StockMetric metric) {
        if (metric.getId() == null) { em.persist(metric); return metric; }
        return em.merge(metric);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<StockMetric> root, ScreeningFilter filter) {
        List<Predicate> predicates = new ArrayList<>();

        LocalDate latestDate = getLatestDate();
        predicates.add(cb.equal(root.get("date"), latestDate));

        if (filter.getSectorId() != null) {
            predicates.add(cb.equal(
                root.get("stock").get("industry").get("sector").get("id"), filter.getSectorId()));
        }
        if (filter.getExchange() != null) {
            predicates.add(cb.equal(root.get("stock").get("exchange"), filter.getExchange()));
        }
        if (filter.getMinPer() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("per"), filter.getMinPer()));
        }
        if (filter.getMaxPer() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("per"), filter.getMaxPer()));
        }
        if (filter.getMinPbr() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("pbr"), filter.getMinPbr()));
        }
        if (filter.getMaxPbr() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("pbr"), filter.getMaxPbr()));
        }
        if (filter.getMinCap() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("stock").get("marketCap"), filter.getMinCap()));
        }
        if (filter.getMaxCap() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("stock").get("marketCap"), filter.getMaxCap()));
        }
        if (filter.getMinDividendYield() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("dividendYield"), filter.getMinDividendYield()));
        }
        if (filter.getMinRoe() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("roe"), filter.getMinRoe()));
        }
        if (filter.getMaxDebtRatio() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("debtRatio"), filter.getMaxDebtRatio()));
        }
        if (filter.getMinRevenueGrowth() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("revenueGrowth"), filter.getMinRevenueGrowth()));
        }
        if (filter.getMinOperatingMargin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("operatingMargin"), filter.getMinOperatingMargin()));
        }

        return predicates;
    }

    private void applySorting(CriteriaBuilder cb, CriteriaQuery<StockMetric> cq, Root<StockMetric> root, String sort) {
        if (sort == null || sort.isEmpty()) {
            cq.orderBy(cb.desc(root.get("stock").get("marketCap")));
            return;
        }
        String[] parts = sort.split(",");
        String field = parts[0];
        boolean desc = parts.length > 1 && "desc".equalsIgnoreCase(parts[1]);

        Path<?> path;
        if ("marketCap".equals(field)) {
            path = root.get("stock").get("marketCap");
        } else {
            path = root.get(field);
        }
        cq.orderBy(desc ? cb.desc(path) : cb.asc(path));
    }
}
