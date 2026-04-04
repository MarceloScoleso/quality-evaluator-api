package br.com.marceloscoleso.quality_evaluator_api.model;
 
import jakarta.persistence.*;
import java.time.LocalDateTime;
 
/**
 * Armazena a integração GitHub de um usuário.
 * Um usuário pode ter no máximo UMA integração ativa (OneToOne).
 *
 * ⚠️  O access_token é dado pessoal e sensível.
 *     Em produção real, criptografe com @Convert + AttributeConverter
 *     usando AES-256 + chave injetada via env var.
 */
@Entity
@Table(name = "github_integrations")
public class GitHubIntegration {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    // ── relação com o usuário ──────────────────────────────────────────────
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
 
    // ── dados do GitHub ────────────────────────────────────────────────────
    @Column(name = "github_user_id", nullable = false)
    private Long githubUserId;
 
    @Column(name = "github_login", nullable = false)
    private String githubLogin;
 
    /**
     * Token OAuth2 do usuário no GitHub.
     * Escopo mínimo necessário: "repo" (repositórios públicos e privados)
     * ou "public_repo" (apenas públicos).
     *
     * TODO produção: criptografar antes de persistir.
     */
    @Column(name = "access_token", nullable = false, length = 512)
    private String accessToken;
 
    @Column(name = "token_scope")
    private String tokenScope;
 
    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;
 
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
 
    // ── construtores ───────────────────────────────────────────────────────
 
    public GitHubIntegration() {}
 
    @PrePersist
    void onCreate() { this.connectedAt = LocalDateTime.now(); }
 
    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
 
    // ── getters / setters ──────────────────────────────────────────────────
 
    public Long getId()                         { return id; }
 
    public User getUser()                       { return user; }
    public void setUser(User user)              { this.user = user; }
 
    public Long getGithubUserId()               { return githubUserId; }
    public void setGithubUserId(Long v)         { this.githubUserId = v; }
 
    public String getGithubLogin()              { return githubLogin; }
    public void setGithubLogin(String v)        { this.githubLogin = v; }
 
    public String getAccessToken()              { return accessToken; }
    public void setAccessToken(String v)        { this.accessToken = v; }
 
    public String getTokenScope()               { return tokenScope; }
    public void setTokenScope(String v)         { this.tokenScope = v; }
 
    public LocalDateTime getConnectedAt()       { return connectedAt; }
    public LocalDateTime getUpdatedAt()         { return updatedAt; }
}
 