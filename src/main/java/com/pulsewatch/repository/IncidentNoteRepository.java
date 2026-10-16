package com.pulsewatch.repository;

import com.pulsewatch.entity.Incident;
import com.pulsewatch.entity.IncidentNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentNoteRepository extends JpaRepository<IncidentNote, Long> {
    List<IncidentNote> findByIncidentOrderByCreatedAtAsc(Incident incident);
}
