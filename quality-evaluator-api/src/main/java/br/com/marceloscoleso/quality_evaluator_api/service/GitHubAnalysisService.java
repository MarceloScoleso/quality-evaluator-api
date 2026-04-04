package br.com.marceloscoleso.quality_evaluator_api.service;
 
import br.com.marceloscoleso.quality_evaluator_api.dto.GitHubRepoDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.RepoAnalysisData;
import br.com.marceloscoleso.quality_evaluator_api.model.Language;
 
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
 
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
 
/**
 * Coleta dados REAIS do repositório via GitHub API e calcula score por pilares.
 *
 * ── Score (0-100) ───────────────────────────────────────────────────────────
 *  Pilar 1 — Testes          25 pts  (hasTests, qtd arquivos de teste)
 *  Pilar 2 — Documentação    20 pts  (README, CONTRIBUTING, CHANGELOG, LICENSE)
 *  Pilar 3 — CI/CD & Config  20 pts  (workflows, Dockerfile, lint, dependency file)
 *  Pilar 4 — Atividade       15 pts  (commits recentes, contribuidores)
 *  Pilar 5 — Estrutura       20 pts  (organização de pastas, tamanho, linguagem)
 * ────────────────────────────────────────────────────────────────────────────
 */
@Service
public class GitHubAnalysisService {
 
    private static final Logger log = LoggerFactory.getLogger(GitHubAnalysisService.class);
 
    private static final String GITHUB_API = "https://api.github.com";
 
    // Pastas de teste reconhecidas
    private static final List<String> TEST_DIRS = List.of(
        "test", "tests", "__tests__", "spec", "specs",
        "src/test", "src/__tests__", "src/tests",
        "e2e", "integration-tests", "unit-tests"
    );
 
    // Arquivos de CI/CD
    private static final List<String> CICD_FILES = List.of(
        ".travis.yml", ".circleci", "Jenkinsfile", ".gitlab-ci.yml",
        "azure-pipelines.yml", ".github"
    );
 
    // Arquivos de dependência por linguagem
    private static final List<String> DEPENDENCY_FILES = List.of(
        "pom.xml", "build.gradle", "package.json", "requirements.txt",
        "Pipfile", "Cargo.toml", "go.mod", "composer.json", "Gemfile",
        "build.sbt", "pubspec.yaml"
    );
 
    // Arquivos de lint/qualidade
    private static final List<String> LINT_FILES = List.of(
        ".eslintrc", ".eslintrc.js", ".eslintrc.json", ".eslintrc.yml",
        "checkstyle.xml", ".pylintrc", ".rubocop.yml", "sonar-project.properties",
        ".editorconfig", "rustfmt.toml"
    );
 
    private final ObjectMapper objectMapper;
    private final HttpClient   httpClient;
 
    public GitHubAnalysisService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient   = HttpClient.newHttpClient();
    }
 
    // ══════════════════════════════════════════════════════════════════════
    // ENTRY POINT
    // ══════════════════════════════════════════════════════════════════════
 
    /**
     * Coleta todos os dados do repo, calcula score e retorna RepoAnalysisData.
     */
    public RepoAnalysisData analyze(String accessToken, GitHubRepoDTO repo) {
 
        RepoAnalysisData data = new RepoAnalysisData();
 
        data.setRepoFullName(repo.getFullName());
        data.setRepoName(repo.getName());
        data.setDescription(repo.getDescription());
        data.setRawLanguage(repo.getLanguage());
        data.setLanguage(mapLanguage(repo.getLanguage()));
        data.setSizeKb(repo.getSizeKb());
        data.setStars(repo.getStars());
        data.setForks(repo.getForks());
        data.setPrivate(repo.isPrivateRepo());
        data.setDefaultBranch(repo.getDefaultBranch() != null ? repo.getDefaultBranch() : "main");
 
        String fullName = repo.getFullName();
        String branch   = data.getDefaultBranch();
 
        // ── 1. Estrutura de arquivos (raiz) ──────────────────────────────
        collectRootStructure(accessToken, fullName, branch, data);
 
        // ── 2. Testes ────────────────────────────────────────────────────
        collectTestData(accessToken, fullName, branch, data);
 
        // ── 3. Documentação ───────────────────────────────────────────────
        collectDocumentation(accessToken, fullName, branch, data);
 
        // ── 4. CI/CD e configuração ───────────────────────────────────────
        detectCiCd(data);  // usa rootFiles já coletados
 
        // ── 5. Atividade de commits ───────────────────────────────────────
        collectCommitActivity(accessToken, fullName, data);
 
        // ── 6. Breakdown de linguagens ────────────────────────────────────
        collectLanguageBreakdown(accessToken, fullName, data);
 
        // ── 7. Amostra de código ──────────────────────────────────────────
        collectCodeSample(accessToken, fullName, branch, data);
 
        // ── 8. Calcular score por pilares ─────────────────────────────────
        calculateScore(data);
 
        return data;
    }
 
    // ══════════════════════════════════════════════════════════════════════
    // COLETA DE DADOS
    // ══════════════════════════════════════════════════════════════════════
 
    private void collectRootStructure(String token, String fullName, String branch, RepoAnalysisData data) {
        try {
            JsonNode items = getJson(token, "/repos/" + fullName + "/contents/?ref=" + branch);
            if (items == null || !items.isArray()) return;
 
            List<String> names = StreamSupport.stream(items.spliterator(), false)
                .map(n -> n.path("name").asText())
                .collect(Collectors.toList());
 
            data.setRootFiles(names);
 
            // detectar arquivos de configuração a partir da raiz
            Set<String> nameSet = names.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
 
            data.setHasLicense(nameSet.contains("license") || nameSet.contains("license.md") || nameSet.contains("license.txt"));
            data.setHasChangelog(nameSet.contains("changelog") || nameSet.contains("changelog.md") || nameSet.contains("history.md"));
            data.setHasContributing(nameSet.contains("contributing") || nameSet.contains("contributing.md"));
            data.setHasDockerfile(nameSet.contains("dockerfile") || nameSet.contains("docker-compose.yml") || nameSet.contains("docker-compose.yaml"));
            data.setHasDependencyFile(DEPENDENCY_FILES.stream().anyMatch(f -> nameSet.contains(f.toLowerCase())));
            data.setHasSecurityPolicy(nameSet.contains("security.md") || nameSet.contains("security"));
 
            boolean hasLint = LINT_FILES.stream().anyMatch(f -> nameSet.contains(f.toLowerCase()));
            data.setHasLintConfig(hasLint);
 
            log.info("Raiz coletada: {} arquivos em {}", names.size(), fullName);
 
        } catch (Exception e) {
            log.warn("Não foi possível coletar estrutura raiz de {}: {}", fullName, e.getMessage());
            data.setRootFiles(List.of());
        }
    }
 
    private void collectTestData(String token, String fullName, String branch, RepoAnalysisData data) {
        List<String> found = new ArrayList<>();
        int fileCount = 0;
 
        for (String dir : TEST_DIRS) {
            try {
                JsonNode resp = getJson(token, "/repos/" + fullName + "/contents/" + dir + "?ref=" + branch);
                if (resp != null && resp.isArray()) {
                    found.add(dir);
                    // contar arquivos de teste
                    fileCount += (int) StreamSupport.stream(resp.spliterator(), false)
                        .filter(n -> {
                            String name = n.path("name").asText().toLowerCase();
                            return name.endsWith("test.java") || name.endsWith("spec.js")
                                || name.endsWith("_test.go") || name.endsWith("_test.py")
                                || name.contains("test") || name.contains("spec");
                        }).count();
                }
            } catch (Exception ignored) {}
        }
 
        // também checar se rootFiles contém pasta de test
        if (data.getRootFiles() != null) {
            for (String rootFile : data.getRootFiles()) {
                String lower = rootFile.toLowerCase();
                if (TEST_DIRS.contains(lower) && !found.contains(lower)) {
                    found.add(lower);
                }
            }
        }
 
        data.setHasTests(!found.isEmpty());
        data.setTestDirsFound(found);
        data.setTestFileCount(fileCount);
 
        log.info("Testes: hasTests={} dirs={} files={}", data.isHasTests(), found, fileCount);
    }
 
    private void collectDocumentation(String token, String fullName, String branch, RepoAnalysisData data) {
        // buscar README
        try {
            JsonNode readme = getJson(token, "/repos/" + fullName + "/readme?ref=" + branch);
            if (readme != null && readme.has("content")) {
                data.setHasReadme(true);
                // decodificar base64 e pegar primeiros 2000 chars
                String encoded = readme.path("content").asText().replaceAll("\\s", "");
                String decoded = new String(Base64.getDecoder().decode(encoded));
                data.setReadmeContent(decoded.length() > 2000 ? decoded.substring(0, 2000) : decoded);
            }
        } catch (Exception e) {
            data.setHasReadme(false);
            log.warn("README não encontrado em {}", fullName);
        }
    }
 
    private void detectCiCd(RepoAnalysisData data) {
        if (data.getRootFiles() == null) return;
 
        Set<String> names = data.getRootFiles().stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
 
        boolean hasCi = CICD_FILES.stream().anyMatch(f -> names.contains(f.toLowerCase()));
 
        // .github/workflows é uma pasta, vamos verificar se ".github" existe na raiz
        if (!hasCi && names.contains(".github")) {
            hasCi = true;
        }
 
        data.setHasCiCd(hasCi);
    }
 
    private void collectCommitActivity(String token, String fullName, RepoAnalysisData data) {
        try {
            // buscar commits recentes (últimas 100 entradas)
            JsonNode commits = getJson(token,
                "/repos/" + fullName + "/commits?per_page=100");
 
            if (commits == null || !commits.isArray()) return;
 
            int total = 0;
            int last30 = 0;
            int last90 = 0;
            Set<String> contributors = new HashSet<>();
            String lastDate = null;
 
            LocalDate now    = LocalDate.now();
            LocalDate ago30  = now.minusDays(30);
            LocalDate ago90  = now.minusDays(90);
 
            for (JsonNode commit : commits) {
                total++;
                String dateStr = commit.path("commit").path("author").path("date").asText("");
                String login   = commit.path("author").path("login").asText("");
 
                if (!login.isBlank()) contributors.add(login);
                if (lastDate == null) lastDate = dateStr;
 
                if (!dateStr.isBlank()) {
                    try {
                        LocalDate commitDate = LocalDate.parse(dateStr.substring(0, 10));
                        if (!commitDate.isBefore(ago30)) last30++;
                        if (!commitDate.isBefore(ago90)) last90++;
                    } catch (Exception ignored) {}
                }
            }
 
            data.setTotalCommits(total);
            data.setCommitLast30Days(last30);
            data.setCommitLast90Days(last90);
            data.setUniqueContributors(contributors.size());
            data.setLastCommitDate(lastDate);
            data.setActivelyMaintained(last90 > 0);
 
            log.info("Commits: total={} 30d={} 90d={} contributors={}",
                total, last30, last90, contributors.size());
 
        } catch (Exception e) {
            log.warn("Não foi possível coletar commits de {}: {}", fullName, e.getMessage());
        }
    }
 
    private void collectLanguageBreakdown(String token, String fullName, RepoAnalysisData data) {
        try {
            JsonNode langs = getJson(token, "/repos/" + fullName + "/languages");
            if (langs == null) return;
 
            Map<String, Integer> breakdown = new LinkedHashMap<>();
            langs.fields().forEachRemaining(e ->
                breakdown.put(e.getKey(), e.getValue().asInt())
            );
            data.setLanguageBreakdown(breakdown);
 
        } catch (Exception e) {
            log.warn("Não foi possível coletar linguagens de {}", fullName);
        }
    }
 
    private void collectCodeSample(String token, String fullName, String branch, RepoAnalysisData data) {
        // detectar arquivo principal com base na linguagem
        String targetFile = detectMainFile(data.getRawLanguage());
        if (targetFile == null) return;
 
        try {
            // buscar tree para encontrar o arquivo
            JsonNode tree = getJson(token,
                "/repos/" + fullName + "/git/trees/" + branch + "?recursive=1");
            if (tree == null) return;
 
            JsonNode treeArr = tree.path("tree");
 
            // coletar arquivos de código (excluindo test, vendor, node_modules)
            List<String> codeFiles = StreamSupport.stream(treeArr.spliterator(), false)
                .filter(n -> n.path("type").asText().equals("blob"))
                .map(n -> n.path("path").asText())
                .filter(path -> matchesLanguageExtension(path, data.getRawLanguage()))
                .filter(path -> !path.contains("test") && !path.contains("vendor")
                             && !path.contains("node_modules") && !path.contains("dist"))
                .limit(20)
                .collect(Collectors.toList());
 
            data.setSourceFiles(codeFiles);
 
            // pegar conteúdo do primeiro arquivo encontrado (max 3000 chars)
            if (!codeFiles.isEmpty()) {
                String filePath = codeFiles.get(0);
                JsonNode fileNode = getJson(token,
                    "/repos/" + fullName + "/contents/" + filePath + "?ref=" + branch);
                if (fileNode != null && fileNode.has("content")) {
                    String encoded = fileNode.path("content").asText().replaceAll("\\s", "");
                    String decoded = new String(Base64.getDecoder().decode(encoded));
                    String sample  = decoded.length() > 3000 ? decoded.substring(0, 3000) : decoded;
                    data.setCodeSample(sample);
                    data.setMainFileContent(filePath);
                    log.info("Amostra de código coletada: {} ({} chars)", filePath, sample.length());
                }
            }
 
        } catch (Exception e) {
            log.warn("Não foi possível coletar amostra de código de {}: {}", fullName, e.getMessage());
        }
    }
 
    // ══════════════════════════════════════════════════════════════════════
    // CÁLCULO DE SCORE POR PILARES
    // ══════════════════════════════════════════════════════════════════════
 
    /**
     * Score total = soma dos 5 pilares (máx 100).
     *
     * Pilar 1 — Testes (25 pts)
     *   +15 se hasTests
     *   +5  se testFileCount >= 5
     *   +5  se testFileCount >= 20
     *
     * Pilar 2 — Documentação (20 pts)
     *   +10 se hasReadme
     *   +4  se README > 500 chars (README detalhado)
     *   +3  se hasLicense
     *   +2  se hasContributing
     *   +1  se hasChangelog
     *
     * Pilar 3 — CI/CD & Config (20 pts)
     *   +8  se hasCiCd
     *   +5  se hasDependencyFile
     *   +4  se hasLintConfig
     *   +3  se hasDockerfile
     *
     * Pilar 4 — Atividade (15 pts)
     *   +8  se isActivelyMaintained (commit nos últimos 90 dias)
     *   +4  se commitLast30Days >= 5
     *   +2  se uniqueContributors >= 2
     *   +1  se uniqueContributors >= 5
     *
     * Pilar 5 — Estrutura (20 pts)
     *   +5  linguagem tipada (Java, Kotlin, TypeScript, Rust, Go, C#)
     *   +5  sizeKb entre 10 e 5000 (não trivial, não gigante)
     *   +4  hasDependencyFile (gerenciador de dependências)
     *   +3  hasSecurityPolicy
     *   +3  forks > 0 (outros projetos reutilizaram)
     */
    private void calculateScore(RepoAnalysisData d) {
 
        // ── Pilar 1: Testes ───────────────────────────────────────────────
        int p1 = 0;
        if (d.isHasTests())            p1 += 15;
        if (d.getTestFileCount() >= 5) p1 += 5;
        if (d.getTestFileCount() >= 20) p1 += 5;
        p1 = Math.min(p1, 25);
 
        // ── Pilar 2: Documentação ─────────────────────────────────────────
        int p2 = 0;
        if (d.isHasReadme())           p2 += 10;
        if (d.getReadmeContent() != null
            && d.getReadmeContent().length() > 500) p2 += 4;
        if (d.isHasLicense())          p2 += 3;
        if (d.isHasContributing())     p2 += 2;
        if (d.isHasChangelog())        p2 += 1;
        p2 = Math.min(p2, 20);
 
        // ── Pilar 3: CI/CD & Config ───────────────────────────────────────
        int p3 = 0;
        if (d.isHasCiCd())             p3 += 8;
        if (d.isHasDependencyFile())   p3 += 5;
        if (d.isHasLintConfig())       p3 += 4;
        if (d.isHasDockerfile())       p3 += 3;
        p3 = Math.min(p3, 20);
 
        // ── Pilar 4: Atividade ────────────────────────────────────────────
        int p4 = 0;
        if (d.isActivelyMaintained())           p4 += 8;
        if (d.getCommitLast30Days() >= 5)       p4 += 4;
        if (d.getUniqueContributors() >= 2)     p4 += 2;
        if (d.getUniqueContributors() >= 5)     p4 += 1;
        p4 = Math.min(p4, 15);
 
        // ── Pilar 5: Estrutura ────────────────────────────────────────────
        int p5 = 0;
        if (isTypedLanguage(d.getRawLanguage()))                  p5 += 5;
        if (d.getSizeKb() >= 10 && d.getSizeKb() <= 5000)        p5 += 5;
        if (d.isHasDependencyFile())                              p5 += 4;
        if (d.isHasSecurityPolicy())                              p5 += 3;
        if (d.getForks() > 0)                                     p5 += 3;
        p5 = Math.min(p5, 20);
 
        d.setPillarTests(p1);
        d.setPillarDocumentation(p2);
        d.setPillarCiCd(p3);
        d.setPillarActivity(p4);
        d.setPillarStructure(p5);
        d.setTotalScore(p1 + p2 + p3 + p4 + p5);
 
        log.info("Score calculado: tests={} docs={} cicd={} activity={} structure={} TOTAL={}",
            p1, p2, p3, p4, p5, d.getTotalScore());
    }
 
    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════
 
    private JsonNode getJson(String token, String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(GITHUB_API + path))
            .header("Authorization",       "Bearer " + token)
            .header("Accept",              "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .GET().build();
 
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
 
        if (resp.statusCode() == 404) return null;
        if (resp.statusCode() != 200) {
            log.warn("GitHub API {} retornou HTTP {}", path, resp.statusCode());
            return null;
        }
 
        return objectMapper.readTree(resp.body());
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
 
    private boolean isTypedLanguage(String lang) {
        if (lang == null) return false;
        return Set.of("Java", "Kotlin", "TypeScript", "Rust", "Go", "C#", "Swift", "Dart")
            .contains(lang);
    }
 
    private String detectMainFile(String lang) {
        if (lang == null) return null;
        return switch (lang) {
            case "Java"       -> "Main.java";
            case "Kotlin"     -> "Main.kt";
            case "TypeScript" -> "index.ts";
            case "JavaScript" -> "index.js";
            case "Python"     -> "main.py";
            case "Go"         -> "main.go";
            case "Rust"       -> "main.rs";
            default           -> null;
        };
    }
 
    private boolean matchesLanguageExtension(String path, String lang) {
        if (lang == null) return false;
        String lower = path.toLowerCase();
        return switch (lang) {
            case "Java"       -> lower.endsWith(".java");
            case "Kotlin"     -> lower.endsWith(".kt");
            case "TypeScript" -> lower.endsWith(".ts") && !lower.endsWith(".d.ts");
            case "JavaScript" -> lower.endsWith(".js") && !lower.endsWith(".min.js");
            case "Python"     -> lower.endsWith(".py");
            case "Go"         -> lower.endsWith(".go");
            case "Rust"       -> lower.endsWith(".rs");
            case "C#"         -> lower.endsWith(".cs");
            case "PHP"        -> lower.endsWith(".php");
            case "Ruby"       -> lower.endsWith(".rb");
            case "Swift"      -> lower.endsWith(".swift");
            default           -> false;
        };
    }
}