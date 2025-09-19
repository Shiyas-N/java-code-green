package com.javacodegreen.backend.staticanalysis;

import java.util.List;
import java.util.Map;

public class Finding {
    private String id;
    private String ruleId;
    private String severity;
    private double energyScore;
    private String message;
    private String file;
    private int startLine;
    private int endLine;
    private String suggestion;
    private Map<String, Object> evidence;
    private List<String> tags;

    public Finding() {}

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public double getEnergyScore() { return energyScore; }
    public void setEnergyScore(double energyScore) { this.energyScore = energyScore; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }

    public int getStartLine() { return startLine; }
    public void setStartLine(int startLine) { this.startLine = startLine; }

    public int getEndLine() { return endLine; }
    public void setEndLine(int endLine) { this.endLine = endLine; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public Map<String, Object> getEvidence() { return evidence; }
    public void setEvidence(Map<String, Object> evidence) { this.evidence = evidence; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
