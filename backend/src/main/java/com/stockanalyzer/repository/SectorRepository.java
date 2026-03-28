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
        if (sector.getId() == null) { em.persist(sector); return sector; }
        return em.merge(sector);
    }
}
