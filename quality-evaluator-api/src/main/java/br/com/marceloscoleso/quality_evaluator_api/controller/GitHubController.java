package br.com.marceloscoleso.quality_evaluator_api.controller;
 
import br.com.marceloscoleso.quality_evaluator_api.dto.*;
import br.com.marceloscoleso.quality_evaluator_api.service.GitHubService;
 
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
 
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
 
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.Map;
import java.util.UUID;
 
@Tag(name = "GitHub Integration", description = "Integração OAuth2 com GitHub")
@RestController
@RequestMapping("/api/github")
@SecurityRequirement(name = "bearerAuth")
public class GitHubController {
 
    private final GitHubService gitHubService;
 
    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }
 
    // ── 1. URL de autorização ──────────────────────────────────────────────
 
    @Operation(
        summary = "Obter URL de autorização GitHub",
        description = """
            Retorna a URL para redirecionar o usuário ao GitHub para autorizar o acesso.
            O frontend deve redirecionar o browser do usuário para esta URL.
            Após autorizar, o GitHub redireciona para redirect_uri com ?code=...&state=...
            """
    )
    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        // state aleatório para proteção CSRF no OAuth flow
        String state = UUID.randomUUID().toString();
        String url   = gitHubService.buildAuthorizationUrl(state);
        return ResponseEntity.ok(Map.of("url", url, "state", state));
    }
 
    // ── 2. Callback: troca code por token ─────────────────────────────────
 
    @Operation(
        summary = "Conectar GitHub (callback OAuth2)",
        description = """
            Recebe o 'code' retornado pelo GitHub após o usuário autorizar.
            O backend troca pelo access_token, salva a integração e retorna status de sucesso.
            
            Fluxo:
            1. Frontend chama GET /api/github/auth-url
            2. Usuário é redirecionado para GitHub
            3. GitHub redireciona para redirect_uri?code=XXX&state=YYY
            4. Frontend chama POST /api/github/callback com { "code": "XXX" }
            5. Backend salva token e retorna { "connected": true, "login": "..." }
            """
    )
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(
            @RequestBody Map<String, @NotBlank String> body) {
 
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "code é obrigatório"));
        }
 
        var integration = gitHubService.exchangeCodeAndSave(code);
        return ResponseEntity.ok(Map.of(
            "connected", true,
            "login",     integration.getGithubLogin(),
            "scope",     integration.getTokenScope() != null ? integration.getTokenScope() : ""
        ));
    }
 
    // ── 3. Status da conexão ───────────────────────────────────────────────
 
    @Operation(summary = "Verificar se GitHub está conectado")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of("connected", gitHubService.isConnected()));
    }
 
    // ── 4. Listar repositórios ─────────────────────────────────────────────
 
    @Operation(
        summary = "Listar repositórios do GitHub",
        description = "Retorna os repositórios do usuário autenticado no GitHub (máx. 100, ordenados por atualização)"
    )
    @GetMapping("/repos")
    public ResponseEntity<List<GitHubRepoDTO>> listRepos() {
        return ResponseEntity.ok(gitHubService.listRepos());
    }
 
    // ── 5. Analisar repositório ────────────────────────────────────────────
 
    @Operation(
        summary = "Analisar repositório GitHub",
        description = """
            Recebe o fullName do repositório (ex: 'marcelo/meu-projeto'),
            busca metadados na API do GitHub, detecta linguagem e testes,
            e cria automaticamente uma avaliação no sistema.
            """
    )
    @PostMapping("/analyze")
    public ResponseEntity<EvaluationResponseDTO> analyze(
            @RequestBody @Valid GitHubAnalyzeRequestDTO request) {
 
        return ResponseEntity.ok(gitHubService.analyzeRepo(request));
    }
 
    // ── 6. Desconectar GitHub ──────────────────────────────────────────────
 
    @Operation(
        summary = "Desconectar GitHub",
        description = "Remove a integração GitHub do usuário. O access_token é deletado do banco."
    )
    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        gitHubService.disconnect();
        return ResponseEntity.noContent().build();
    }
}