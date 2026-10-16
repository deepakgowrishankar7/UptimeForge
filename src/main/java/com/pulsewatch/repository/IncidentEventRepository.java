package com.pulsewatch.repository;

import com.pulsewatch.entity.Incident;
import com.pulsewatch.entity.IncidentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentEventRepository extends JpaRepository<IncidentEvent, Long> {
    List<IncidentEvent> findByIncidentOrderByCreatedAtAsc(Incident incident);
}
