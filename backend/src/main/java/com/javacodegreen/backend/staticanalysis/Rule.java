package com.javacodegreen.backend.staticanalysis;

import java.util.List;
import java.util.Map;

public class Rule {
    private String id;
    private String description;
    private String severity;
    private Map<String, Object> match; // dynamic match object (node, operator, ancestor, type, etc.)
    private String suggestion;
    private List<String> tags;

    public Rule() {}

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Map<String, Object> getMatch() { return match; }
    public void setMatch(Map<String, Object> match) { this.match = match; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
