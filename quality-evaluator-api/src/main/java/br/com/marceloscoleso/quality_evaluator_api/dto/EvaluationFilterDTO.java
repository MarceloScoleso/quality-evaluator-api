package br.com.marceloscoleso.quality_evaluator_api.dto;

import br.com.marceloscoleso.quality_evaluator_api.model.Language;
import br.com.marceloscoleso.quality_evaluator_api.model.Classification;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public class EvaluationFilterDTO {

    @Schema(description = "Data inicial (yyyy-MM-dd)", example = "2024-01-01")
    private LocalDate startDate;

    @Schema(description = "Data final (yyyy-MM-dd)", example = "2024-12-31")
    private LocalDate endDate;

    @Schema(description = "Nome do projeto (busca parcial)", example = "quality")
    private String projectName;

    @Schema(description = "Classificação da avaliação", example = "BOM")
    private Classification classification;

    @Schema(description = "Nota mínima", example = "5")
    private Integer minScore;

    @Schema(description = "Nota máxima", example = "10")
    private Integer maxScore;

    @Schema(description = "Linguagem do projeto")
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

    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
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
