package br.com.marceloscoleso.quality_evaluator_api.dto;

import br.com.marceloscoleso.quality_evaluator_api.model.Language;

import java.time.LocalDateTime;

public class EvaluationResponseDTO {

    private Long id;
    private String projectName;
    private Language language;
    private Integer score;
    private String classification;
    private String analyzedBy;
    private LocalDateTime createdAt;
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getProjectName() {
        return projectName;
    }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    public Language getLanguage() {
        return language;
    }
    public void setLanguage(Language language) {
        this.language = language;
    }
    public Integer getScore() {
        return score;
    }
    public void setScore(Integer score) {
        this.score = score;
    }
    public String getClassification() {
        return classification;
    }
    public void setClassification(String classification) {
        this.classification = classification;
    }
    public String getAnalyzedBy() {
        return analyzedBy;
    }
    public void setAnalyzedBy(String analyzedBy) {
        this.analyzedBy = analyzedBy;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    
}
