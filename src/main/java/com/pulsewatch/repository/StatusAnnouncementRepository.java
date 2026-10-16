package com.pulsewatch.repository;

import com.pulsewatch.entity.StatusAnnouncement;
import com.pulsewatch.entity.StatusPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusAnnouncementRepository extends JpaRepository<StatusAnnouncement, Long> {
    List<StatusAnnouncement> findByStatusPageOrderByStartsAtDesc(StatusPage statusPage);
}
