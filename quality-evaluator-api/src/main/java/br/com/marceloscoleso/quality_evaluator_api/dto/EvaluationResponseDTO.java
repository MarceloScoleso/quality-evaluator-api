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

    @Schema(description = "Indica se o projeto possui testes automatizados", example = "true")
    private boolean hasTests;

    @Schema(description = "Indica se o projeto utiliza Git para versionamento", example = "true")
    private boolean usesGit;    

    @Schema(description = "Pontuação obtida", example = "100")
    private Integer score;

    @Schema(description = "Classificação final do projeto", example = "EXCELENTE")
    private Classification classification;

    @Schema(description = "Responsável pela análise", example = "Marcelo")
    private String analyzedBy;

    @Schema(description = "Data e hora da criação da avaliação")
    private LocalDateTime createdAt;

    @Schema(description = "Quantidade de linhas de código do projeto", example = "250")
    private Integer linesOfCode;

    @Schema(description = "Complexidade do projeto (1 a 5)", example = "3")
    private Integer complexity;

    @Schema(description = "Descrição detalhada do projeto ou análise da IA", example = "Este projeto implementa um sistema de avaliação de qualidade de software com métricas avançadas, testes automatizados e versionamento Git.")
    private String description;

    // =========================
    // GETTERS E SETTERS
    // =========================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }

    public boolean isHasTests() { return hasTests; }
    public void setHasTests(boolean hasTests) { this.hasTests = hasTests; }

    public boolean isUsesGit() { return usesGit; }
    public void setUsesGit(boolean usesGit) { this.usesGit = usesGit; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Classification getClassification() { return classification; }
    public void setClassification(Classification classification) { this.classification = classification; }

    public String getAnalyzedBy() { return analyzedBy; }
    public void setAnalyzedBy(String analyzedBy) { this.analyzedBy = analyzedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getLinesOfCode() { return linesOfCode; }
    public void setLinesOfCode(Integer linesOfCode) { this.linesOfCode = linesOfCode; }

    public Integer getComplexity() { return complexity; }
    public void setComplexity(Integer complexity) { this.complexity = complexity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

}