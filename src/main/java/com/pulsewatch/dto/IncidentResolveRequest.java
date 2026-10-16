package com.pulsewatch.dto;

public class IncidentResolveRequest {
    private String resolutionNotes;

    public IncidentResolveRequest() {}

    public IncidentResolveRequest(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}
