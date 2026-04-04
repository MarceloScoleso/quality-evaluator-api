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
 
    private static final String GITHUB_API      = "https://api.github.com";
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
 
    @Value("${github.client-id}")
    private String clientId;
 
    @Value("${github.client-secret}")
    private String clientSecret;
 
    @Value("${github.redirect-uri}")
    private String redirectUri;
 
    private final UserRepository             userRepository;
    private final GitHubIntegrationRepository integrationRepository;
    private final EvaluationService          evaluationService;
    private final ObjectMapper               objectMapper;
    private final HttpClient                 httpClient;
 
    public GitHubService(UserRepository userRepository,
                         GitHubIntegrationRepository integrationRepository,
                         EvaluationService evaluationService,
                         ObjectMapper objectMapper) {
        this.userRepository        = userRepository;
        this.integrationRepository = integrationRepository;
        this.evaluationService     = evaluationService;
        this.objectMapper          = objectMapper;
        this.httpClient            = HttpClient.newHttpClient();
    }
 
    // ── 1. URL de autorização (frontend redireciona o usuário) ─────────────
 
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
 
        // 2a. Trocar code por token
        GitHubTokenResponseDTO tokenResp = exchangeCode(code);
        if (tokenResp.hasError()) {
            throw new BusinessException(
                "GitHub OAuth error: " + tokenResp.getErrorDescription()
            );
        }
 
        String accessToken = tokenResp.getAccessToken();
 
        // 2b. Buscar perfil do usuário no GitHub
        GitHubUserDTO ghUser = fetchGitHubUser(accessToken);
 
        // 2c. Upsert da integração
        GitHubIntegration integration = integrationRepository
            .findByUser(user)
            .orElseGet(GitHubIntegration::new);
 
        integration.setUser(user);
        integration.setGithubUserId(ghUser.getId());
        integration.setGithubLogin(ghUser.getLogin());
        integration.setAccessToken(accessToken);
        integration.setTokenScope(tokenResp.getScope());
 
        GitHubIntegration saved = integrationRepository.save(integration);
 
        log.info("GitHub conectado. User ID={} GitHub login={}",
                 user.getId(), ghUser.getLogin());
 
        return saved;
    }
 
    // ── 3. Listar repositórios ─────────────────────────────────────────────
 
    public List<GitHubRepoDTO> listRepos() {
 
        User user = getAuthenticatedUser();
        String token = getTokenOrThrow(user);
 
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API + "/user/repos?per_page=100&sort=updated&affiliation=owner"))
                .header("Authorization", "Bearer " + token)
                .header("Accept",        "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();
 
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
 
            if (resp.statusCode() == 401) {
                // Token expirado ou revogado
                integrationRepository.deleteByUser(user);
                throw new BusinessException("Token GitHub expirado. Reconecte sua conta.");
            }
 
            if (resp.statusCode() != 200) {
                throw new BusinessException("Erro ao buscar repositórios: HTTP " + resp.statusCode());
            }
 
            GitHubRepoDTO[] repos = objectMapper.readValue(resp.body(), GitHubRepoDTO[].class);
            return Arrays.asList(repos);
 
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao listar repos GitHub", e);
            throw new BusinessException("Não foi possível acessar a API do GitHub.");
        }
    }
 
    // ── 4. Analisar repositório → cria avaliação ──────────────────────────
 
    @Transactional
    public EvaluationResponseDTO analyzeRepo(GitHubAnalyzeRequestDTO request) {
 
        User user = getAuthenticatedUser();
        String token = getTokenOrThrow(user);
 
        // 4a. Buscar dados do repo
        GitHubRepoDTO repo = fetchRepo(token, request.getRepoFullName());
 
        // 4b. Detectar presença de testes (heurística por linguagem)
        boolean hasTests = detectTests(token, request.getRepoFullName(), repo.getLanguage());
 
        // 4c. Detectar linguagem → mapear para enum Language
        Language language = mapLanguage(repo.getLanguage());
 
        // 4d. Estimar linhas de código pelo tamanho do repo (size em KB)
        int estimatedLines = estimateLines(repo.getSizeKb());
 
        // 4e. Montar EvaluationRequestDTO e criar avaliação
        EvaluationRequestDTO dto = new EvaluationRequestDTO();
        dto.setProjectName(repo.getName());
        dto.setLanguage(language);
        dto.setLinesOfCode(estimatedLines);
        dto.setComplexity(estimateComplexity(repo.getSizeKb(), hasTests));
        dto.setHasTests(hasTests);
        dto.setUsesGit(true); // é um repo GitHub, logo usa Git
        dto.setAnalyzedBy(request.getAnalyzedBy() != null
            ? request.getAnalyzedBy()
            : user.getName());
        dto.setAiLang(request.getAiLang());
        // description null → gerada automaticamente pelo DescriptionGeneratorService
 
        log.info("Analisando repo={} lang={} lines={} tests={}",
                 repo.getFullName(), language, estimatedLines, hasTests);
 
        return evaluationService.create(dto);
    }
 
    // ── 5. Status da integração ────────────────────────────────────────────
 
    public boolean isConnected() {
        User user = getAuthenticatedUser();
        return integrationRepository.existsByUser(user);
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
     * Heurística: verifica se existe pasta de testes comum no repositório.
     * Chama GET /repos/{owner}/{repo}/contents para detectar pastas típicas.
     */
    private boolean detectTests(String token, String fullName, String language) {
        // pastas de teste mais comuns por linguagem
        List<String> testDirs = List.of("test", "tests", "__tests__", "spec", "src/test", "src/__tests__");
 
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API + "/repos/" + fullName + "/contents/"))
                .header("Authorization", "Bearer " + token)
                .header("Accept",        "application/vnd.github+json")
                .GET().build();
 
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return false;
 
            String body = resp.body().toLowerCase();
            return testDirs.stream().anyMatch(dir -> body.contains("\"" + dir + "\""));
 
        } catch (Exception e) {
            log.warn("Não foi possível detectar testes para {}", fullName);
            return false;
        }
    }
 
    private Language mapLanguage(String ghLang) {
        if (ghLang == null) return Language.OTHER;
        return switch (ghLang.toUpperCase()) {
            case "JAVA"        -> Language.JAVA;
            case "KOTLIN"      -> Language.KOTLIN;
            case "C#"          -> Language.CSHARP;
            case "JAVASCRIPT"  -> Language.JAVASCRIPT;
            case "TYPESCRIPT"  -> Language.TYPESCRIPT;
            case "PYTHON"      -> Language.PYTHON;
            case "GO"          -> Language.GO;
            case "PHP"         -> Language.PHP;
            case "RUBY"        -> Language.RUBY;
            case "SWIFT"       -> Language.SWIFT;
            case "C"           -> Language.C;
            case "C++"         -> Language.CPP;
            case "RUST"        -> Language.RUST;
            case "DART"        -> Language.DART;
            default            -> Language.OTHER;
        };
    }
 
    /** Estima linhas de código a partir do tamanho em KB reportado pelo GitHub. */
    private int estimateLines(int sizeKb) {
        // heurística: ~30 linhas por KB (média empírica para código-fonte)
        return Math.max(50, Math.min(sizeKb * 30, 50_000));
    }
 
    /** Estima complexidade (1-5) baseada no tamanho e presença de testes. */
    private int estimateComplexity(int sizeKb, boolean hasTests) {
        int base;
        if      (sizeKb < 50)   base = 1;
        else if (sizeKb < 200)  base = 2;
        else if (sizeKb < 1000) base = 3;
        else if (sizeKb < 5000) base = 4;
        else                    base = 5;
 
        // Projetos com testes tendem a ser melhor estruturados
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