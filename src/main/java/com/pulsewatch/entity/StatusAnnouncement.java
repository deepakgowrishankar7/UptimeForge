package com.pulsewatch.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "status_announcements")
public class StatusAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_page_id", nullable = false)
    private StatusPage statusPage;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt = Instant.now();

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public StatusAnnouncement() {}

    public StatusAnnouncement(StatusPage statusPage, String title, String message, Instant startsAt, Instant endsAt) {
        this.statusPage = statusPage;
        this.title = title;
        this.message = message;
        this.startsAt = startsAt != null ? startsAt : Instant.now();
        this.endsAt = endsAt;
        this.createdAt = Instant.now();
    }

    public boolean isActiveNow() {
        Instant now = Instant.now();
        if (now.isBefore(startsAt)) return false;
        return endsAt == null || now.isBefore(endsAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StatusPage getStatusPage() { return statusPage; }
    public void setStatusPage(StatusPage statusPage) { this.statusPage = statusPage; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }

    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
