package com.stockanalyzer.repository;

import com.stockanalyzer.entity.SectorMetric;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class SectorMetricRepository {

    @PersistenceContext
    private EntityManager em;

    public List<SectorMetric> findLatestAll() {
        LocalDate latestDate = getLatestDate();
        if (latestDate == null) return List.of();
        return em.createQuery(
                "SELECT sm FROM SectorMetric sm JOIN FETCH sm.sector WHERE sm.date = :date ORDER BY sm.totalMarketCap DESC",
                SectorMetric.class)
                .setParameter("date", latestDate)
                .getResultList();
    }

    public SectorMetric findLatestBySector(Long sectorId) {
        LocalDate latestDate = getLatestDate();
        if (latestDate == null) return null;
        List<SectorMetric> results = em.createQuery(
                "SELECT sm FROM SectorMetric sm WHERE sm.sector.id = :sectorId AND sm.date = :date",
                SectorMetric.class)
                .setParameter("sectorId", sectorId)
                .setParameter("date", latestDate)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public List<SectorMetric> findBySectorAndDateRange(Long sectorId, LocalDate from, LocalDate to) {
        return em.createQuery(
                "SELECT sm FROM SectorMetric sm WHERE sm.sector.id = :sectorId AND sm.date >= :from AND sm.date <= :to ORDER BY sm.date ASC",
                SectorMetric.class)
                .setParameter("sectorId", sectorId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    private LocalDate getLatestDate() {
        List<LocalDate> results = em.createQuery("SELECT MAX(sm.date) FROM SectorMetric sm", LocalDate.class).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Transactional
    public SectorMetric save(SectorMetric metric) {
        if (metric.getId() == null) { em.persist(metric); return metric; }
        return em.merge(metric);
    }
}
