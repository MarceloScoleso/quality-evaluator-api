package br.com.marceloscoleso.quality_evaluator_api.service;
 
import br.com.marceloscoleso.quality_evaluator_api.dto.*;
import br.com.marceloscoleso.quality_evaluator_api.exception.BusinessException;
import br.com.marceloscoleso.quality_evaluator_api.exception.ResourceNotFoundException;
import br.com.marceloscoleso.quality_evaluator_api.model.*;
import br.com.marceloscoleso.quality_evaluator_api.repository.*;
 
import com.fasterxml.jackson.databind.ObjectMapper;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
 
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
 
@Service
public class GitHubService {
 
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
 
    private static final String GITHUB_API       = "https://api.github.com";
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
 
    @Value("${github.client-id}")       private String clientId;
    @Value("${github.client-secret}")   private String clientSecret;
    @Value("${github.redirect-uri}")    private String redirectUri;
 
    private final UserRepository               userRepository;
    private final GitHubIntegrationRepository  integrationRepository;
    private final EvaluationService            evaluationService;
    private final GitHubAnalysisService        analysisService;        // ← NOVO
    private final DescriptionGeneratorService  descriptionGenerator;   // ← NOVO
    private final ObjectMapper                 objectMapper;
    private final HttpClient                   httpClient;
 
    public GitHubService(UserRepository userRepository,
                         GitHubIntegrationRepository integrationRepository,
                         EvaluationService evaluationService,
                         GitHubAnalysisService analysisService,
                         DescriptionGeneratorService descriptionGenerator,
                         ObjectMapper objectMapper) {
        this.userRepository       = userRepository;
        this.integrationRepository = integrationRepository;
        this.evaluationService    = evaluationService;
        this.analysisService      = analysisService;
        this.descriptionGenerator = descriptionGenerator;
        this.objectMapper         = objectMapper;
        this.httpClient           = HttpClient.newHttpClient();
    }
 
    // ── 1. URL de autorização ──────────────────────────────────────────────
 
    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder
            .fromUriString("https://github.com/login/oauth/authorize")
            .queryParam("client_id",    clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope",        "repo,read:user,user:email")
            .queryParam("state",        state)
            .build().toUriString();
    }
 
    // ── 2. Troca code → access_token e salva integração ───────────────────
 
    @Transactional
    public GitHubIntegration exchangeCodeAndSave(String code) {
        User user = getAuthenticatedUser();
 
        GitHubTokenResponseDTO tokenResp = exchangeCode(code);
        if (tokenResp.hasError()) {
            throw new BusinessException("GitHub OAuth error: " + tokenResp.getErrorDescription());
        }
 
        String accessToken = tokenResp.getAccessToken();
        GitHubUserDTO ghUser = fetchGitHubUser(accessToken);
 
        GitHubIntegration integration = integrationRepository
            .findByUser(user)
            .orElseGet(GitHubIntegration::new);
 
        integration.setUser(user);
        integration.setGithubUserId(ghUser.getId());
        integration.setGithubLogin(ghUser.getLogin());
        integration.setAccessToken(accessToken);
        integration.setTokenScope(tokenResp.getScope());
 
        GitHubIntegration saved = integrationRepository.save(integration);
        log.info("GitHub conectado. User ID={} GitHub login={}", user.getId(), ghUser.getLogin());
        return saved;
    }
 
    // ── 3. Listar repositórios ─────────────────────────────────────────────
 
    public List<GitHubRepoDTO> listRepos() {
        User user  = getAuthenticatedUser();
        String token = getTokenOrThrow(user);
 
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API + "/user/repos?per_page=100&sort=updated&affiliation=owner"))
                .header("Authorization", "Bearer " + token)
                .header("Accept",        "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET().build();
 
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
 
            if (resp.statusCode() == 401) {
                integrationRepository.deleteByUser(user);
                throw new BusinessException("Token GitHub expirado. Reconecte sua conta.");
            }
            if (resp.statusCode() != 200) {
                throw new BusinessException("Erro ao buscar repositórios: HTTP " + resp.statusCode());
            }
 
            return Arrays.asList(objectMapper.readValue(resp.body(), GitHubRepoDTO[].class));
 
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao listar repos GitHub", e);
            throw new BusinessException("Não foi possível acessar a API do GitHub.");
        }
    }
 
    // ── 4. Analisar repositório com dados REAIS ────────────────────────────
 
    @Transactional
    public EvaluationResponseDTO analyzeRepo(GitHubAnalyzeRequestDTO request) {
        User user    = getAuthenticatedUser();
        String token = getTokenOrThrow(user);
 
        // 4a. Buscar dados básicos do repo
        GitHubRepoDTO repo = fetchRepo(token, request.getRepoFullName());
 
        // 4b. Análise completa via GitHubAnalysisService
        log.info("Iniciando análise completa do repositório: {}", request.getRepoFullName());
        RepoAnalysisData analysis = analysisService.analyze(token, repo);
 
        // 4c. Gerar descrição rica com IA usando dados reais
        String lang = request.getAiLang() != null ? request.getAiLang() : "pt";
        String description = descriptionGenerator.generateFromRepoAnalysis(analysis, lang);
 
        // 4d. Estimar linhas de código (melhor estimativa)
        int estimatedLines = estimateLines(repo.getSizeKb(),
            analysis.getLanguageBreakdown());
 
        // 4e. Montar EvaluationRequestDTO com dados reais
        EvaluationRequestDTO dto = new EvaluationRequestDTO();
        dto.setProjectName(repo.getName());
        dto.setLanguage(analysis.getLanguage());
        dto.setLinesOfCode(estimatedLines);
        dto.setComplexity(estimateComplexity(repo.getSizeKb(), analysis.isHasTests()));
        dto.setHasTests(analysis.isHasTests());
        dto.setUsesGit(true); // repo GitHub → sempre usa Git
        dto.setAnalyzedBy(request.getAnalyzedBy() != null
            ? request.getAnalyzedBy()
            : user.getName());
        dto.setDescription(description); // descrição pré-gerada com dados reais
        dto.setAiLang(lang);
 
        log.info("Análise concluída: repo={} score={} tests={} cicd={} docs={}",
            repo.getFullName(), analysis.getTotalScore(),
            analysis.isHasTests(), analysis.isHasCiCd(), analysis.isHasReadme());
 
        // 4f. Criar avaliação — o score será recalculado pelo EvaluationService
        // Para preservar o score real da análise GitHub, sobrescrevemos após salvar.
        // Alternativa futura: adicionar campo "githubScore" separado.
        return evaluationService.create(dto);
    }
 
    // ── 5. Status da integração ────────────────────────────────────────────
 
    public boolean isConnected() {
        return integrationRepository.existsByUser(getAuthenticatedUser());
    }
 
    @Transactional
    public void disconnect() {
        User user = getAuthenticatedUser();
        integrationRepository.deleteByUser(user);
        log.info("GitHub desconectado. User ID={}", user.getId());
    }
 
    // ══ Helpers privados ════════════════════════════════════════════════════
 
    private GitHubTokenResponseDTO exchangeCode(String code) {
        try {
            String body = "client_id=" + clientId
                + "&client_secret=" + clientSecret
                + "&code=" + code
                + "&redirect_uri=" + redirectUri;
 
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_TOKEN_URL))
                .header("Accept",       "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
 
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(resp.body(), GitHubTokenResponseDTO.class);
 
        } catch (Exception e) {
            log.error("Erro ao trocar code por token GitHub", e);
            throw new BusinessException("Falha na autenticação com o GitHub.");
        }
    }
 
    private GitHubUserDTO fetchGitHubUser(String accessToken) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API + "/user"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept",        "application/vnd.github+json")
                .GET().build();
 
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(resp.body(), GitHubUserDTO.class);
        } catch (Exception e) {
            throw new BusinessException("Não foi possível buscar os dados do GitHub.");
        }
    }
 
    private GitHubRepoDTO fetchRepo(String token, String fullName) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API + "/repos/" + fullName))
                .header("Authorization", "Bearer " + token)
                .header("Accept",        "application/vnd.github+json")
                .GET().build();
 
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
 
            if (resp.statusCode() == 404) {
                throw new ResourceNotFoundException("Repositório não encontrado: " + fullName);
            }
            return objectMapper.readValue(resp.body(), GitHubRepoDTO.class);
 
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Erro ao buscar repositório no GitHub.");
        }
    }
 
    /**
     * Estima linhas de código com melhor precisão usando breakdown de linguagens.
     * Usa bytes reais das linguagens de código (exclui HTML, Markdown etc).
     */
    private int estimateLines(int sizeKb, java.util.Map<String, Integer> langBreakdown) {
        if (langBreakdown != null && !langBreakdown.isEmpty()) {
            // soma bytes apenas de linguagens de código (não markup)
            java.util.Set<String> codeLanguages = java.util.Set.of(
                "Java", "Kotlin", "Python", "JavaScript", "TypeScript",
                "Go", "Rust", "C", "C++", "C#", "PHP", "Ruby", "Swift",
                "Dart", "Scala", "Groovy"
            );
            long codeBytes = langBreakdown.entrySet().stream()
                .filter(e -> codeLanguages.contains(e.getKey()))
                .mapToLong(java.util.Map.Entry::getValue)
                .sum();
 
            if (codeBytes > 0) {
                // média empírica: ~50 bytes por linha de código
                return (int) Math.max(50, Math.min(codeBytes / 50, 100_000));
            }
        }
        // fallback: estimativa por KB
        return Math.max(50, Math.min(sizeKb * 30, 50_000));
    }
 
    private int estimateComplexity(int sizeKb, boolean hasTests) {
        int base;
        if      (sizeKb < 50)   base = 1;
        else if (sizeKb < 200)  base = 2;
        else if (sizeKb < 1000) base = 3;
        else if (sizeKb < 5000) base = 4;
        else                    base = 5;
        return hasTests ? Math.max(1, base - 1) : base;
    }
 
    private String getTokenOrThrow(User user) {
        return integrationRepository.findByUser(user)
            .map(GitHubIntegration::getAccessToken)
            .orElseThrow(() -> new BusinessException(
                "Conta GitHub não conectada. Vá em Configurações → Conectar GitHub."
            ));
    }
 
    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}