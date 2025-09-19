package com.javacodegreen.backend.staticanalysis;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AnalysisResult {
    private String jobId;
    private Map<String, String> project;
    private Instant analyzedAt;
    private List<Finding> findings;
    private Map<String, Integer> summary;

    public AnalysisResult() {}

    // getters & setters
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public Map<String, String> getProject() { return project; }
    public void setProject(Map<String, String> project) { this.project = project; }

    public Instant getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(Instant analyzedAt) { this.analyzedAt = analyzedAt; }

    public List<Finding> getFindings() { return findings; }
    public void setFindings(List<Finding> findings) { this.findings = findings; }

    public Map<String, Integer> getSummary() { return summary; }
    public void setSummary(Map<String, Integer> summary) { this.summary = summary; }
}
