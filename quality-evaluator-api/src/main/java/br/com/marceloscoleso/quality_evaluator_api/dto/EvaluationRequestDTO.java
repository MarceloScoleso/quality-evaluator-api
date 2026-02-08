package br.com.marceloscoleso.quality_evaluator_api.dto;


import br.com.marceloscoleso.quality_evaluator_api.model.Language;
import jakarta.validation.constraints.*;

public class EvaluationRequestDTO {

    @NotBlank
    private String projectName;

    @NotNull
    private Language language;

    @Min(0)
    @Max(100)
    private Integer score;

    @NotBlank
    private String classification;

    @NotBlank
    private String analyzedBy;

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

    
}