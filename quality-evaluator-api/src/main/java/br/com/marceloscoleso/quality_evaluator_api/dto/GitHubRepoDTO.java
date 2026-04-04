package br.com.marceloscoleso.quality_evaluator_api.dto;
 
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
 
// ── Repositório retornado pela GitHub API ──────────────────────────────────
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepoDTO {
 
    private Long id;
    private String name;
 
    @JsonProperty("full_name")
    private String fullName;
 
    private String description;
 
    @JsonProperty("html_url")
    private String htmlUrl;
 
    @JsonProperty("stargazers_count")
    private int stars;
 
    @JsonProperty("forks_count")
    private int forks;
 
    private String language;
 
    @JsonProperty("private")
    private boolean privateRepo;
 
    @JsonProperty("default_branch")
    private String defaultBranch;
 
    @JsonProperty("size")
    private int sizeKb; // tamanho em KB — proxy para linhas de código
 
    @JsonProperty("updated_at")
    private String updatedAt;
 
    // ── getters ────────────────────────────────────────────────────────────
 
    public Long getId()               { return id; }
    public String getName()           { return name; }
    public String getFullName()       { return fullName; }
    public String getDescription()    { return description; }
    public String getHtmlUrl()        { return htmlUrl; }
    public int getStars()             { return stars; }
    public int getForks()             { return forks; }
    public String getLanguage()       { return language; }
    public boolean isPrivateRepo()    { return privateRepo; }
    public String getDefaultBranch()  { return defaultBranch; }
    public int getSizeKb()            { return sizeKb; }
    public String getUpdatedAt()      { return updatedAt; }
}