package br.com.marceloscoleso.quality_evaluator_api.service.impl;
 
import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.RepoAnalysisData;
import br.com.marceloscoleso.quality_evaluator_api.model.Classification;
import br.com.marceloscoleso.quality_evaluator_api.service.DescriptionGeneratorService;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
 
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
 
@Service
public class DescriptionGeneratorServiceImpl implements DescriptionGeneratorService {
 
    private static final Logger log = LoggerFactory.getLogger(DescriptionGeneratorServiceImpl.class);
 
    private final WebClient webClient;
    private final String model;
 
    public DescriptionGeneratorServiceImpl(
            @Value("${groq.api-key}") String apiKey,
            @Value("${groq.model:llama-3.3-70b-versatile}") String model
    ) {
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
 
    // ══════════════════════════════════════════════════════════════════════
    // MODO MANUAL (formulário)
    // ══════════════════════════════════════════════════════════════════════
 
    @Override
    public String generate(EvaluationRequestDTO dto, int score, Classification classification, String lang) {
        try {
            String languageName = resolveLanguageName(lang);
 
            Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 300,
                "messages", List.of(
                    Map.of("role", "system", "content",
                        "You are a software quality expert. " +
                        "Generate objective and professional technical descriptions " +
                        "in the following language: " + languageName + ". " +
                        "Maximum 3 sentences. Be direct and avoid generic introductions."),
                    Map.of("role", "user", "content", buildManualPrompt(dto, score, classification))
                )
            );
 
            String text = callGroq(body);
            log.info("✅ Descrição manual gerada para: {}", dto.getProjectName());
            return text;
 
        } catch (Exception e) {
            log.error("❌ Falha ao chamar Groq (manual), usando fallback. Erro: {}", e.getMessage());
            return generateFallback(dto, score, classification, lang);
        }
    }
 
    // ══════════════════════════════════════════════════════════════════════
    // MODO GITHUB (análise real do repositório)
    // ══════════════════════════════════════════════════════════════════════
 
    @Override
    public String generateFromRepoAnalysis(RepoAnalysisData data, String lang) {
        try {
            String languageName = resolveLanguageName(lang);
 
            Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 500,
                "messages", List.of(
                    Map.of("role", "system", "content", buildSystemPrompt(languageName)),
                    Map.of("role", "user", "content", buildRepoPrompt(data))
                )
            );
 
            String text = callGroq(body);
            log.info("✅ Descrição GitHub gerada para: {} (score={})",
                data.getRepoFullName(), data.getTotalScore());
            return text;
 
        } catch (Exception e) {
            log.error("❌ Falha ao chamar Groq (GitHub), usando fallback. Erro: {}", e.getMessage());
            return buildRepoFallback(data, lang);
        }
    }
 
    // ══════════════════════════════════════════════════════════════════════
    // PROMPTS
    // ══════════════════════════════════════════════════════════════════════
 
    private String buildSystemPrompt(String language) {
        return """
            You are a senior software engineer and code quality expert.
            You analyze GitHub repositories and provide professional, actionable feedback.
            Always respond in: %s.
            Structure your response with:
            1. A 1-sentence summary of the project and its overall quality.
            2. 2-3 strong points found in the repository.
            3. 2-3 specific improvement recommendations based on the data provided.
            Be direct, technical and avoid generic statements.
            Maximum 5 sentences total.
            """.formatted(language);
    }
 
    private String buildRepoPrompt(RepoAnalysisData d) {
        StringBuilder sb = new StringBuilder();
 
        sb.append("=== REPOSITORY ANALYSIS ===\n");
        sb.append("Repository: ").append(d.getRepoFullName()).append("\n");
        sb.append("Description: ").append(d.getDescription() != null ? d.getDescription() : "N/A").append("\n");
        sb.append("Language: ").append(d.getRawLanguage() != null ? d.getRawLanguage() : "Unknown").append("\n");
        sb.append("Size: ").append(d.getSizeKb()).append(" KB\n");
        sb.append("Stars: ").append(d.getStars()).append(" | Forks: ").append(d.getForks()).append("\n\n");
 
        sb.append("=== SCORE BREAKDOWN ===\n");
        sb.append("Total Score: ").append(d.getTotalScore()).append("/100\n");
        sb.append("  Tests:         ").append(d.getPillarTests()).append("/25\n");
        sb.append("  Documentation: ").append(d.getPillarDocumentation()).append("/20\n");
        sb.append("  CI/CD:         ").append(d.getPillarCiCd()).append("/20\n");
        sb.append("  Activity:      ").append(d.getPillarActivity()).append("/15\n");
        sb.append("  Structure:     ").append(d.getPillarStructure()).append("/20\n\n");
 
        sb.append("=== TESTING ===\n");
        sb.append("Has Tests: ").append(d.isHasTests() ? "Yes" : "No").append("\n");
        if (d.getTestDirsFound() != null && !d.getTestDirsFound().isEmpty()) {
            sb.append("Test Dirs: ").append(String.join(", ", d.getTestDirsFound())).append("\n");
        }
        sb.append("Test Files Found: ").append(d.getTestFileCount()).append("\n\n");
 
        sb.append("=== DOCUMENTATION ===\n");
        sb.append("README: ").append(d.isHasReadme() ? "Yes" : "No").append("\n");
        sb.append("License: ").append(d.isHasLicense() ? "Yes" : "No").append("\n");
        sb.append("Contributing Guide: ").append(d.isHasContributing() ? "Yes" : "No").append("\n");
        sb.append("Changelog: ").append(d.isHasChangelog() ? "Yes" : "No").append("\n");
        sb.append("Security Policy: ").append(d.isHasSecurityPolicy() ? "Yes" : "No").append("\n\n");
 
        sb.append("=== CI/CD & CONFIGURATION ===\n");
        sb.append("CI/CD Pipeline: ").append(d.isHasCiCd() ? "Yes" : "No").append("\n");
        sb.append("Dependency File: ").append(d.isHasDependencyFile() ? "Yes" : "No").append("\n");
        sb.append("Lint Config: ").append(d.isHasLintConfig() ? "Yes" : "No").append("\n");
        sb.append("Dockerfile: ").append(d.isHasDockerfile() ? "Yes" : "No").append("\n\n");
 
        sb.append("=== ACTIVITY ===\n");
        sb.append("Actively Maintained: ").append(d.isActivelyMaintained() ? "Yes" : "No").append("\n");
        sb.append("Commits (last 30 days): ").append(d.getCommitLast30Days()).append("\n");
        sb.append("Commits (last 90 days): ").append(d.getCommitLast90Days()).append("\n");
        sb.append("Unique Contributors: ").append(d.getUniqueContributors()).append("\n");
        sb.append("Last Commit: ").append(d.getLastCommitDate() != null ? d.getLastCommitDate() : "Unknown").append("\n\n");
 
        // incluir amostra de código apenas se disponível e não muito grande
        if (d.getCodeSample() != null && !d.getCodeSample().isBlank()) {
            sb.append("=== CODE SAMPLE (").append(d.getMainFileContent()).append(") ===\n");
            // limitar amostra para não estourar o contexto
            String sample = d.getCodeSample().length() > 1500
                ? d.getCodeSample().substring(0, 1500) + "\n... [truncated]"
                : d.getCodeSample();
            sb.append(sample).append("\n\n");
        }
 
        // incluir início do README se disponível
        if (d.getReadmeContent() != null && !d.getReadmeContent().isBlank()) {
            sb.append("=== README EXCERPT ===\n");
            String readme = d.getReadmeContent().length() > 800
                ? d.getReadmeContent().substring(0, 800) + "\n... [truncated]"
                : d.getReadmeContent();
            sb.append(readme).append("\n\n");
        }
 
        sb.append("Based on this data, provide your professional quality assessment.");
 
        return sb.toString();
    }
 
    private String buildManualPrompt(EvaluationRequestDTO dto, int score, Classification classification) {
        return """
            Evaluate the project with the following characteristics:
            - Name: %s
            - Language: %s
            - Lines of code: %d
            - Complexity: %d/5
            - Has automated tests: %s
            - Uses Git: %s
            - Final score: %d/100
            - Classification: %s
 
            Generate a professional technical description highlighting the strong points, \
            weak points and one main improvement recommendation. \
            Maximum 3 sentences. Be direct and objective.
            """.formatted(
            dto.getProjectName(), dto.getLanguage().name(),
            dto.getLinesOfCode(), dto.getComplexity(),
            Boolean.TRUE.equals(dto.getHasTests()) ? "Yes" : "No",
            Boolean.TRUE.equals(dto.getUsesGit()) ? "Yes" : "No",
            score, classification.name()
        );
    }
 
    // ══════════════════════════════════════════════════════════════════════
    // CHAMADA GROQ
    // ══════════════════════════════════════════════════════════════════════
 
    @SuppressWarnings("unchecked")
    private String callGroq(Map<String, Object> body) {
        Map response = webClient.post()
            .uri("/chat/completions")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
 
        List<Map> choices = (List<Map>) response.get("choices");
        Map message = (Map) choices.get(0).get("message");
        return ((String) message.get("content")).trim();
    }
 
    // ══════════════════════════════════════════════════════════════════════
    // FALLBACKS
    // ══════════════════════════════════════════════════════════════════════
 
    private String buildRepoFallback(RepoAnalysisData d, String lang) {
        Classification cls = classifyScore(d.getTotalScore());
        String quality = switch (lang) {
            case "en" -> switch (cls) {
                case EXCELENTE -> "excellent"; case BOM -> "good";
                case REGULAR -> "regular"; case RUIM -> "poor";
            };
            case "es" -> switch (cls) {
                case EXCELENTE -> "excelente"; case BOM -> "buena";
                case REGULAR -> "regular"; case RUIM -> "baja";
            };
            default -> switch (cls) {
                case EXCELENTE -> "excelente"; case BOM -> "boa";
                case REGULAR -> "regular"; case RUIM -> "baixa";
            };
        };
 
        String tests = switch (lang) {
            case "en" -> d.isHasTests()
                ? "The repository includes automated tests."
                : "No automated tests were detected.";
            case "es" -> d.isHasTests()
                ? "El repositorio incluye pruebas automatizadas."
                : "No se detectaron pruebas automatizadas.";
            default -> d.isHasTests()
                ? "O repositório possui testes automatizados."
                : "Não foram detectados testes automatizados.";
        };
 
        String ci = switch (lang) {
            case "en" -> d.isHasCiCd() ? "CI/CD pipeline detected." : "No CI/CD pipeline found.";
            case "es" -> d.isHasCiCd() ? "Pipeline de CI/CD detectado." : "Sin pipeline de CI/CD.";
            default   -> d.isHasCiCd() ? "Pipeline de CI/CD detectado." : "Sem pipeline de CI/CD.";
        };
 
        return switch (lang) {
            case "en" -> "The repository %s scored %d/100, showing %s quality. %s %s"
                .formatted(d.getRepoFullName(), d.getTotalScore(), quality, tests, ci);
            case "es" -> "El repositorio %s obtuvo %d/100, con calidad %s. %s %s"
                .formatted(d.getRepoFullName(), d.getTotalScore(), quality, tests, ci);
            default -> "O repositório %s obteve %d/100, apresentando qualidade %s. %s %s"
                .formatted(d.getRepoFullName(), d.getTotalScore(), quality, tests, ci);
        };
    }
 
    private String generateFallback(EvaluationRequestDTO dto, int score, Classification cls, String lang) {
        return switch (lang) {
            case "en" -> "Project %s in %s scored %d/100.".formatted(dto.getProjectName(), dto.getLanguage(), score);
            case "es" -> "Proyecto %s en %s obtuvo %d/100.".formatted(dto.getProjectName(), dto.getLanguage(), score);
            default   -> "Projeto %s em %s obteve %d/100.".formatted(dto.getProjectName(), dto.getLanguage(), score);
        };
    }
 
    private Classification classifyScore(int score) {
        if (score >= 80) return Classification.EXCELENTE;
        if (score >= 60) return Classification.BOM;
        if (score >= 40) return Classification.REGULAR;
        return Classification.RUIM;
    }
 
    private String resolveLanguageName(String lang) {
        return switch (lang) {
            case "en" -> "English";
            case "es" -> "Spanish";
            default   -> "Brazilian Portuguese";
        };
    }
}