package com.pulsewatch.dto;

import com.pulsewatch.entity.IncidentNote;
import java.time.Instant;

public class IncidentNoteResponse {
    private Long id;
    private Long incidentId;
    private Long authorId;
    private String authorName;
    private String note;
    private Instant createdAt;

    public IncidentNoteResponse() {}

    public static IncidentNoteResponse fromEntity(IncidentNote n) {
        if (n == null) return null;
        IncidentNoteResponse resp = new IncidentNoteResponse();
        resp.setId(n.getId());
        if (n.getIncident() != null) {
            resp.setIncidentId(n.getIncident().getId());
        }
        if (n.getAuthor() != null) {
            resp.setAuthorId(n.getAuthor().getId());
            resp.setAuthorName(n.getAuthor().getName());
        }
        resp.setNote(n.getNote());
        resp.setCreatedAt(n.getCreatedAt());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
