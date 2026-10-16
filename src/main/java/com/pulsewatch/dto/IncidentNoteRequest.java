package com.pulsewatch.dto;

import jakarta.validation.constraints.NotBlank;

public class IncidentNoteRequest {

    @NotBlank(message = "Note content is required")
    private String note;

    public IncidentNoteRequest() {}

    public IncidentNoteRequest(String note) {
        this.note = note;
    }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
