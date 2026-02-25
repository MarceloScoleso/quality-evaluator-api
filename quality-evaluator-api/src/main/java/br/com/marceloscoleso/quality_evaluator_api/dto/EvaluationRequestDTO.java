package br.com.marceloscoleso.quality_evaluator_api.dto;

import br.com.marceloscoleso.quality_evaluator_api.model.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Dados de entrada para avaliação de um projeto")
public class EvaluationRequestDTO {

    @Schema(description = "Nome do projeto avaliado", example = "Quality Evaluator API")
    @NotBlank
    private String projectName;

    @Schema(description = "Linguagem utilizada no projeto", example = "JAVA")
    @NotNull
    private Language language;

    @Schema(description = "Quantidade de linhas de código", example = "250")
    @NotNull
    @Min(1)
    private Integer linesOfCode;

    @Schema(description = "Nível de complexidade do projeto (1 a 5)", example = "2")
    @NotNull
    @Min(1)
    @Max(5)
    private Integer complexity;

    @Schema(description = "Indica se o projeto possui testes automatizados", example = "true")
    @NotNull
    private Boolean hasTests;

    @Schema(description = "Indica se o projeto utiliza controle de versão (Git)", example = "true")
    @NotNull
    private Boolean usesGit;

    @Schema(description = "Responsável pela análise", example = "Marcelo")
    @NotBlank
    private String analyzedBy;

    @Schema(description = "Descrição opcional do projeto", example = "Sistema de avaliação de software")
    private String description;

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

    public Integer getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(Integer linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public Integer getComplexity() {
        return complexity;
    }

    public void setComplexity(Integer complexity) {
        this.complexity = complexity;
    }

    public Boolean getHasTests() {
        return hasTests;
    }

    public void setHasTests(Boolean hasTests) {
        this.hasTests = hasTests;
    }

    public Boolean getUsesGit() {
        return usesGit;
    }

    public void setUsesGit(Boolean usesGit) {
        this.usesGit = usesGit;
    }

    public String getAnalyzedBy() {
        return analyzedBy;
    }

    public void setAnalyzedBy(String analyzedBy) {
        this.analyzedBy = analyzedBy;
    }

    public String getDescription() { 
        return description; 
    }

    public void setDescription(String description) { 
        this.description = description; 
    }
}
