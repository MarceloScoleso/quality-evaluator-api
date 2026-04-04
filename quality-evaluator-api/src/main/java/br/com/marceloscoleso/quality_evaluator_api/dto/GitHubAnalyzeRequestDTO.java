package br.com.marceloscoleso.quality_evaluator_api.dto;
 
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
 
// ── Payload para análise de repositório ───────────────────────────────────
public class GitHubAnalyzeRequestDTO {
 
    @NotBlank
    private String repoFullName; // ex: "marcelo/meu-projeto"
 
    private String analyzedBy;
 
    private String aiLang = "pt";
 
    public String getRepoFullName()       { return repoFullName; }
    public void setRepoFullName(String v) { this.repoFullName = v; }
 
    public String getAnalyzedBy()         { return analyzedBy; }
    public void setAnalyzedBy(String v)   { this.analyzedBy = v; }
 
    public String getAiLang()             { return aiLang; }
    public void setAiLang(String v)       { this.aiLang = v; }
}