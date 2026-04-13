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
        return em.createQuery("SELECT i FROM Industry i WHERE i.sector.id = :sectorId", Industry.class)
                .setParameter("sectorId", sectorId).getResultList();
    }
    public Industry findById(Long id) {
        return em.find(Industry.class, id);
    }
    public Industry findByName(String name) {
        List<Industry> results = em.createQuery(
                "SELECT i FROM Industry i WHERE i.name = :name ORDER BY i.id ASC", Industry.class)
                .setParameter("name", name)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
    @Transactional
    public Industry save(Industry industry) {
        if (industry.getId() == null) { em.persist(industry); return industry; }
        return em.merge(industry);
    }
}
