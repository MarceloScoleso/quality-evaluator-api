package br.com.marceloscoleso.quality_evaluator_api.dto;
 
import br.com.marceloscoleso.quality_evaluator_api.model.Language;
 
import java.util.List;
import java.util.Map;
 
/**
 * Dados coletados da API do GitHub para análise completa de um repositório.
 * Preenchido pelo GitHubAnalysisService antes de calcular o score.
 */
public class RepoAnalysisData {
 
    // ── Dados básicos ──────────────────────────────────────────────────────
    private String repoFullName;
    private String repoName;
    private String description;
    private Language language;
    private String rawLanguage;
    private int sizeKb;
    private int stars;
    private int forks;
    private boolean isPrivate;
    private String defaultBranch;
 
    // ── Estrutura de arquivos (raiz) ───────────────────────────────────────
    private List<String> rootFiles;      // nomes dos arquivos/pastas na raiz
    private List<String> sourceFiles;    // amostra de arquivos de código
 
    // ── Testes ────────────────────────────────────────────────────────────
    private boolean hasTests;
    private List<String> testDirsFound; // ex: ["test", "src/test"]
    private int testFileCount;
 
    // ── Documentação ──────────────────────────────────────────────────────
    private boolean hasReadme;
    private String readmeContent;        // primeiros 2000 chars do README
    private boolean hasContributing;
    private boolean hasChangelog;
    private boolean hasLicense;
 
    // ── CI/CD e configuração ──────────────────────────────────────────────
    private boolean hasCiCd;             // .github/workflows, .travis.yml, etc.
    private boolean hasDockerfile;
    private boolean hasDependencyFile;   // pom.xml, package.json, requirements.txt
    private boolean hasLintConfig;       // .eslintrc, checkstyle, pylint
    private boolean hasSecurityPolicy;   // SECURITY.md
 
    // ── Atividade de commits ──────────────────────────────────────────────
    private int totalCommits;            // últimos 100 (limite da API)
    private int commitLast30Days;
    private int commitLast90Days;
    private int uniqueContributors;
    private String lastCommitDate;
    private boolean isActivelyMaintained; // commit nos últimos 90 dias
 
    // ── Linguagens do repo ────────────────────────────────────────────────
    private Map<String, Integer> languageBreakdown; // lang → bytes
 
    // ── Amostra de código para IA ─────────────────────────────────────────
    private String codeSample;           // trecho de código-fonte (max 3000 chars)
    private String mainFileContent;      // conteúdo do arquivo principal detectado
 
    // ── Score calculado por pilar ─────────────────────────────────────────
    private int pillarTests;         // 0-25
    private int pillarDocumentation; // 0-20
    private int pillarCiCd;          // 0-20
    private int pillarActivity;      // 0-15
    private int pillarStructure;     // 0-20
    private int totalScore;          // 0-100
 
    // ═══════════════════════════════════════════════════════════════════════
    // Getters e Setters
    // ═══════════════════════════════════════════════════════════════════════
 
    public String getRepoFullName()                       { return repoFullName; }
    public void setRepoFullName(String v)                 { this.repoFullName = v; }
 
    public String getRepoName()                           { return repoName; }
    public void setRepoName(String v)                     { this.repoName = v; }
 
    public String getDescription()                        { return description; }
    public void setDescription(String v)                  { this.description = v; }
 
    public Language getLanguage()                         { return language; }
    public void setLanguage(Language v)                   { this.language = v; }
 
    public String getRawLanguage()                        { return rawLanguage; }
    public void setRawLanguage(String v)                  { this.rawLanguage = v; }
 
    public int getSizeKb()                                { return sizeKb; }
    public void setSizeKb(int v)                          { this.sizeKb = v; }
 
    public int getStars()                                 { return stars; }
    public void setStars(int v)                           { this.stars = v; }
 
    public int getForks()                                 { return forks; }
    public void setForks(int v)                           { this.forks = v; }
 
    public boolean isPrivate()                            { return isPrivate; }
    public void setPrivate(boolean v)                     { this.isPrivate = v; }
 
    public String getDefaultBranch()                      { return defaultBranch; }
    public void setDefaultBranch(String v)                { this.defaultBranch = v; }
 
    public List<String> getRootFiles()                    { return rootFiles; }
    public void setRootFiles(List<String> v)              { this.rootFiles = v; }
 
    public List<String> getSourceFiles()                  { return sourceFiles; }
    public void setSourceFiles(List<String> v)            { this.sourceFiles = v; }
 
    public boolean isHasTests()                           { return hasTests; }
    public void setHasTests(boolean v)                    { this.hasTests = v; }
 
    public List<String> getTestDirsFound()                { return testDirsFound; }
    public void setTestDirsFound(List<String> v)          { this.testDirsFound = v; }
 
    public int getTestFileCount()                         { return testFileCount; }
    public void setTestFileCount(int v)                   { this.testFileCount = v; }
 
    public boolean isHasReadme()                          { return hasReadme; }
    public void setHasReadme(boolean v)                   { this.hasReadme = v; }
 
    public String getReadmeContent()                      { return readmeContent; }
    public void setReadmeContent(String v)                { this.readmeContent = v; }
 
    public boolean isHasContributing()                    { return hasContributing; }
    public void setHasContributing(boolean v)             { this.hasContributing = v; }
 
    public boolean isHasChangelog()                       { return hasChangelog; }
    public void setHasChangelog(boolean v)                { this.hasChangelog = v; }
 
    public boolean isHasLicense()                         { return hasLicense; }
    public void setHasLicense(boolean v)                  { this.hasLicense = v; }
 
    public boolean isHasCiCd()                            { return hasCiCd; }
    public void setHasCiCd(boolean v)                     { this.hasCiCd = v; }
 
    public boolean isHasDockerfile()                      { return hasDockerfile; }
    public void setHasDockerfile(boolean v)               { this.hasDockerfile = v; }
 
    public boolean isHasDependencyFile()                  { return hasDependencyFile; }
    public void setHasDependencyFile(boolean v)           { this.hasDependencyFile = v; }
 
    public boolean isHasLintConfig()                      { return hasLintConfig; }
    public void setHasLintConfig(boolean v)               { this.hasLintConfig = v; }
 
    public boolean isHasSecurityPolicy()                  { return hasSecurityPolicy; }
    public void setHasSecurityPolicy(boolean v)           { this.hasSecurityPolicy = v; }
 
    public int getTotalCommits()                          { return totalCommits; }
    public void setTotalCommits(int v)                    { this.totalCommits = v; }
 
    public int getCommitLast30Days()                      { return commitLast30Days; }
    public void setCommitLast30Days(int v)                { this.commitLast30Days = v; }
 
    public int getCommitLast90Days()                      { return commitLast90Days; }
    public void setCommitLast90Days(int v)                { this.commitLast90Days = v; }
 
    public int getUniqueContributors()                    { return uniqueContributors; }
    public void setUniqueContributors(int v)              { this.uniqueContributors = v; }
 
    public String getLastCommitDate()                     { return lastCommitDate; }
    public void setLastCommitDate(String v)               { this.lastCommitDate = v; }
 
    public boolean isActivelyMaintained()                 { return isActivelyMaintained; }
    public void setActivelyMaintained(boolean v)          { this.isActivelyMaintained = v; }
 
    public Map<String, Integer> getLanguageBreakdown()    { return languageBreakdown; }
    public void setLanguageBreakdown(Map<String, Integer> v) { this.languageBreakdown = v; }
 
    public String getCodeSample()                         { return codeSample; }
    public void setCodeSample(String v)                   { this.codeSample = v; }
 
    public String getMainFileContent()                    { return mainFileContent; }
    public void setMainFileContent(String v)              { this.mainFileContent = v; }
 
    public int getPillarTests()                           { return pillarTests; }
    public void setPillarTests(int v)                     { this.pillarTests = v; }
 
    public int getPillarDocumentation()                   { return pillarDocumentation; }
    public void setPillarDocumentation(int v)             { this.pillarDocumentation = v; }
 
    public int getPillarCiCd()                            { return pillarCiCd; }
    public void setPillarCiCd(int v)                      { this.pillarCiCd = v; }
 
    public int getPillarActivity()                        { return pillarActivity; }
    public void setPillarActivity(int v)                  { this.pillarActivity = v; }
 
    public int getPillarStructure()                       { return pillarStructure; }
    public void setPillarStructure(int v)                 { this.pillarStructure = v; }
 
    public int getTotalScore()                            { return totalScore; }
    public void setTotalScore(int v)                      { this.totalScore = v; }
}
 