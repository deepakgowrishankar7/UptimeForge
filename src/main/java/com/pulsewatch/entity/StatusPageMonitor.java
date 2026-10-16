package com.pulsewatch.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "status_page_monitors", uniqueConstraints = {
    @UniqueConstraint(name = "uq_spm_pair", columnNames = {"status_page_id", "monitor_id"})
})
public class StatusPageMonitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_page_id", nullable = false)
    private StatusPage statusPage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    public StatusPageMonitor() {}

    public StatusPageMonitor(StatusPage statusPage, Monitor monitor) {
        this.statusPage = statusPage;
        this.monitor = monitor;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StatusPage getStatusPage() { return statusPage; }
    public void setStatusPage(StatusPage statusPage) { this.statusPage = statusPage; }

    public Monitor getMonitor() { return monitor; }
    public void setMonitor(Monitor monitor) { this.monitor = monitor; }
}
