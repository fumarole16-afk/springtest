package com.stockanalyzer.repository;

import com.stockanalyzer.entity.IndustryMetric;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class IndustryMetricRepository {

    @PersistenceContext
    private EntityManager em;

    public List<IndustryMetric> findLatestBySector(Long sectorId) {
        LocalDate latestDate = getLatestDate();
        if (latestDate == null) return List.of();
        return em.createQuery(
                "SELECT im FROM IndustryMetric im JOIN FETCH im.industry WHERE im.industry.sector.id = :sectorId AND im.date = :date",
                IndustryMetric.class)
                .setParameter("sectorId", sectorId)
                .setParameter("date", latestDate)
                .getResultList();
    }

    public IndustryMetric findLatestByIndustry(Long industryId) {
        LocalDate latestDate = getLatestDate();
        if (latestDate == null) return null;
        List<IndustryMetric> results = em.createQuery(
                "SELECT im FROM IndustryMetric im WHERE im.industry.id = :industryId AND im.date = :date",
                IndustryMetric.class)
                .setParameter("industryId", industryId)
                .setParameter("date", latestDate)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private LocalDate getLatestDate() {
        List<LocalDate> results = em.createQuery("SELECT MAX(im.date) FROM IndustryMetric im", LocalDate.class).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Transactional
    public IndustryMetric save(IndustryMetric metric) {
        if (metric.getId() == null) { em.persist(metric); return metric; }
        return em.merge(metric);
    }

    @Transactional
    public int deleteByDate(LocalDate date) {
        return em.createQuery("DELETE FROM IndustryMetric im WHERE im.date = :date")
                .setParameter("date", date)
                .executeUpdate();
    }
}
