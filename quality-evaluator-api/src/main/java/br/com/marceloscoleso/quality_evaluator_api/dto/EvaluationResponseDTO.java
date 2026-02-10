package br.com.marceloscoleso.quality_evaluator_api.dto;

import br.com.marceloscoleso.quality_evaluator_api.model.Classification;
import br.com.marceloscoleso.quality_evaluator_api.model.Language;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Dados retornados após a criação ou consulta de uma avaliação")
public class EvaluationResponseDTO {

    @Schema(description = "ID da avaliação", example = "1")
    private Long id;

    @Schema(description = "Nome do projeto avaliado", example = "Quality Evaluator API")
    private String projectName;

    @Schema(description = "Linguagem utilizada no projeto", example = "JAVA")
    private Language language;

    @Schema(description = "Pontuação obtida", example = "85")
    private Integer score;

    @Schema(description = "Classificação final do projeto", example = "EXCELENTE")
    private Classification classification;

    @Schema(description = "Responsável pela análise", example = "Marcelo")
    private String analyzedBy;

    @Schema(description = "Data e hora da criação da avaliação")
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

    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
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
