package br.com.marceloscoleso.quality_evaluator_api.model;



import java.time.LocalDate;

public class EvaluationFilter {

    private LocalDate startDate;
    private LocalDate endDate;
    private String projectName;
    private String classification;
    private Integer minScore;
    private Integer maxScore;
    private Language language;

    public LocalDate getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    public LocalDate getEndDate() {
        return endDate;
    }
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    public String getProjectName() {
        return projectName;
    }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    public String getClassification() {
        return classification;
    }
    public void setClassification(String classification) {
        this.classification = classification;
    }
    public Integer getMinScore() {
        return minScore;
    }
    public void setMinScore(Integer minScore) {
        this.minScore = minScore;
    }
    public Integer getMaxScore() {
        return maxScore;
    }
    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }
    public Language getLanguage() {
        return language;
    }
    public void setLanguage(Language language) {
        this.language = language;
    }

   
}