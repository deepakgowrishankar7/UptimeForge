package com.pulsewatch.repository;

import com.pulsewatch.entity.Monitor;
import com.pulsewatch.entity.StatusPage;
import com.pulsewatch.entity.StatusPageMonitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusPageMonitorRepository extends JpaRepository<StatusPageMonitor, Long> {
    List<StatusPageMonitor> findByStatusPage(StatusPage statusPage);
    void deleteByStatusPage(StatusPage statusPage);
    boolean existsByStatusPageAndMonitor(StatusPage statusPage, Monitor monitor);
}
