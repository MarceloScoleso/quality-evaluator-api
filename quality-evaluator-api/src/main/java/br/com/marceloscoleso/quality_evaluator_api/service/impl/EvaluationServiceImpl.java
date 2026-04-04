package br.com.marceloscoleso.quality_evaluator_api.service.impl;
 
import br.com.marceloscoleso.quality_evaluator_api.dto.*;
import br.com.marceloscoleso.quality_evaluator_api.model.*;
import br.com.marceloscoleso.quality_evaluator_api.exception.*;
import br.com.marceloscoleso.quality_evaluator_api.repository.*;
import br.com.marceloscoleso.quality_evaluator_api.service.DescriptionGeneratorService;
import br.com.marceloscoleso.quality_evaluator_api.service.EvaluationService;
import br.com.marceloscoleso.quality_evaluator_api.util.CsvExporterApi;
 
import io.micrometer.core.instrument.*;
import org.slf4j.*;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
 
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
 
@Service
public class EvaluationServiceImpl implements EvaluationService {
 
    private static final Logger log = LoggerFactory.getLogger(EvaluationServiceImpl.class);
 
    private final EvaluationRepository evaluationRepository;
    private final MeterRegistry meterRegistry;
    private final UserRepository userRepository;
    private final DescriptionGeneratorService descriptionGeneratorService;
 
    public EvaluationServiceImpl(
            EvaluationRepository evaluationRepository,
            MeterRegistry meterRegistry,
            UserRepository userRepository,
            DescriptionGeneratorService descriptionGeneratorService
    ) {
        this.evaluationRepository = evaluationRepository;
        this.meterRegistry = meterRegistry;
        this.userRepository = userRepository;
        this.descriptionGeneratorService = descriptionGeneratorService;
    }
 
    private User getAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
 
        log.info("🔐 AUTH OBJECT: {}", auth);
 
        if (auth == null) {
            log.error("❌ Authentication está NULL");
            throw new RuntimeException("Usuário não autenticado");
        }
 
        String email = auth.getName();
 
        log.info("📧 EMAIL DO TOKEN: {}", email);
 
        Optional<User> userOpt = userRepository.findByEmail(email);
 
        if (userOpt.isEmpty()) {
            log.error("❌ Usuário NÃO encontrado no banco para email: {}", email);
            userRepository.findAll().forEach(u ->
                log.info("👤 USER NO BANCO: {}", u.getEmail())
            );
            throw new ResourceNotFoundException("Usuário não encontrado");
        }
 
        User user = userOpt.get();
        log.info("✅ Usuário encontrado: ID={} EMAIL={}", user.getId(), user.getEmail());
        return user;
    }
 
    @Override
    @Caching(evict = {
            @CacheEvict(value = "evaluations", allEntries = true),
            @CacheEvict(value = "evaluationStats", allEntries = true)
    })
    public EvaluationResponseDTO create(EvaluationRequestDTO dto) {
 
        if (dto.getLanguage() == null) {
            throw new InvalidLanguageException("Linguagem inválida");
        }
 
        return io.micrometer.core.instrument.Timer
            .builder("business.evaluations.create.time")
            .register(meterRegistry)
            .record(() -> {
 
                int score;

if (dto.getScore() != null) {
    // 🔥 veio do GitHub
    score = dto.getScore();
} else {
    // 🧾 avaliação manual
    score = calculateScore(dto);
}

Classification classification = classify(score);
 
                Evaluation evaluation = new Evaluation();
                evaluation.setProjectName(dto.getProjectName());
                evaluation.setLanguage(dto.getLanguage());
                evaluation.setScore(score);
                evaluation.setClassification(classification);
                evaluation.setAnalyzedBy(dto.getAnalyzedBy());
                evaluation.setCreatedAt(LocalDateTime.now());
                evaluation.setHasTests(dto.getHasTests());
                evaluation.setUsesGit(dto.getUsesGit());
                evaluation.setLinesOfCode(dto.getLinesOfCode());
                evaluation.setComplexity(dto.getComplexity());
 
                if (dto.getDescription() != null && !dto.getDescription().trim().isEmpty()) {
                    evaluation.setDescription(dto.getDescription());
                } else {
                    String lang = dto.getAiLang() != null ? dto.getAiLang() : "pt";
                    evaluation.setDescription(
                        descriptionGeneratorService.generate(dto, score, classification, lang)
                    );
                }
 
                User user = getAuthenticatedUser();
                evaluation.setUser(user);
 
                Evaluation saved = evaluationRepository.save(evaluation);
 
                Counter.builder("business.evaluations.created")
                        .tag("classification", classification.name())
                        .register(meterRegistry)
                        .increment();
 
                return toResponseDTO(saved);
            });
    }
 
    @Override
    public Page<EvaluationResponseDTO> findAll(Pageable pageable) {
        log.info("📥 CHAMOU findAll");
 
        User user = getAuthenticatedUser();
 
        log.info("🔍 Buscando avaliações para user ID={}", user.getId());
 
        Page<Evaluation> page = evaluationRepository.findAllByUserId(user.getId(), pageable);
 
        log.info("📊 TOTAL ENCONTRADO: {}", page.getTotalElements());
 
        return page.map(e -> {
            try {
                return toResponseDTO(e);
            } catch (Exception ex) {
                log.error("💥 ERRO AO CONVERTER Evaluation ID=" + e.getId(), ex);
                throw ex;
            }
        });
    }
 
    @Override
    @Cacheable(value = "evaluation", key = "#id + '-' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().authentication.name")
    public EvaluationResponseDTO findById(Long id) {
 
        User user = getAuthenticatedUser();
 
        return evaluationRepository.findByIdAndUserId(id, user.getId())
                .map(this::toResponseDTO)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Avaliação não encontrada"));
    }
 
    @Override
    public Page<EvaluationResponseDTO> filter(EvaluationFilterDTO filter, Pageable pageable) {
 
        validateFilter(filter);
 
        User user = getAuthenticatedUser();
 
        List<EvaluationResponseDTO> filtered = evaluationRepository.findAllByUserId(user.getId()).stream()
 
                .filter(e -> {
                    LocalDate date = e.getCreatedAt().toLocalDate();
                    if (filter.getStartDate() != null && date.isBefore(filter.getStartDate())) return false;
                    if (filter.getEndDate() != null && date.isAfter(filter.getEndDate())) return false;
                    return true;
                })
 
                .filter(e ->
                        filter.getProjectName() == null ||
                                e.getProjectName().toLowerCase()
                                        .contains(filter.getProjectName().toLowerCase())
                )
 
                .filter(e ->
                        filter.getLanguage() == null ||
                                e.getLanguage() == filter.getLanguage()
                )
 
                .filter(e -> {
                    if (filter.getMinScore() != null && e.getScore() < filter.getMinScore()) return false;
                    if (filter.getMaxScore() != null && e.getScore() > filter.getMaxScore()) return false;
                    return true;
                })
 
                .filter(e ->
                        filter.getClassification() == null ||
                                e.getClassification() == filter.getClassification()
                )
 
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
 
                .map(this::toResponseDTO)
                .toList();
 
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
 
        List<EvaluationResponseDTO> pageContent =
                start > filtered.size() ? List.of() : filtered.subList(start, end);
 
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }
 
    @Override
    public byte[] exportCsv(EvaluationFilterDTO filter) {
 
        validateFilter(filter);
 
        User user = getAuthenticatedUser();
 
        List<Evaluation> evaluations =
                evaluationRepository.findAllByUserId(user.getId());
 
        if (evaluations.isEmpty()) {
            throw new BusinessException("Nenhuma avaliação encontrada para exportação");
        }
 
        return CsvExporterApi.export(evaluations);
    }
 
    @Override
    @Cacheable(value = "evaluationStats",
            key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().authentication.name")
    public EvaluationStatsDTO getStats() {
 
        User user = getAuthenticatedUser();
 
        List<Evaluation> evaluations =
                evaluationRepository.findAllByUserId(user.getId());
 
        long total = evaluations.size();
 
        double average = evaluations.stream()
                .mapToInt(Evaluation::getScore)
                .average()
                .orElse(0.0);
 
        long excellentCount = evaluations.stream()
                .filter(e -> e.getClassification() == Classification.EXCELENTE)
                .count();
 
        return new EvaluationStatsDTO(
                total,
                Math.round(average),
                excellentCount
        );
    }
 
    @Override
    @Caching(evict = {
        @CacheEvict(value = "evaluations", allEntries = true),
        @CacheEvict(value = "evaluation", key = "#id + '-' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().authentication.name"),
        @CacheEvict(value = "evaluationStats", allEntries = true)
    })
    public EvaluationResponseDTO update(Long id, EvaluationRequestDTO dto) {
 
        User user = getAuthenticatedUser();
 
        Evaluation evaluation = evaluationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Avaliação não encontrada"));
 
        int score;

if (dto.getScore() != null) {
    // 🔥 veio do GitHub
    score = dto.getScore();
} else {
    // 🧾 avaliação manual
    score = calculateScore(dto);
}

Classification classification = classify(score);
        evaluation.setProjectName(dto.getProjectName());
        evaluation.setLanguage(dto.getLanguage());
        evaluation.setLinesOfCode(dto.getLinesOfCode());
        evaluation.setComplexity(dto.getComplexity());
        evaluation.setHasTests(dto.getHasTests());
        evaluation.setUsesGit(dto.getUsesGit());
        evaluation.setAnalyzedBy(dto.getAnalyzedBy());
        evaluation.setScore(score);
        evaluation.setClassification(classification);
 
        if (dto.getDescription() != null && !dto.getDescription().trim().isEmpty()) {
            evaluation.setDescription(dto.getDescription());
        } else {
            String lang = dto.getAiLang() != null ? dto.getAiLang() : "pt";
            evaluation.setDescription(
                descriptionGeneratorService.generate(dto, score, classification, lang)
            );
        }
 
        Evaluation updated = evaluationRepository.save(evaluation);
 
        Counter.builder("business.evaluations.updated")
                .tag("classification", classification.name())
                .register(meterRegistry)
                .increment();
 
        return toResponseDTO(updated);
    }
 
    @Override
    @Caching(evict = {
        @CacheEvict(value = "evaluations", allEntries = true),
        @CacheEvict(value = "evaluation", key = "#id + '-' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().authentication.name"),
        @CacheEvict(value = "evaluationStats", allEntries = true)
    })
    public void delete(Long id) {
 
        User user = getAuthenticatedUser();
 
        Evaluation evaluation = evaluationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Avaliação não encontrada"));
 
        evaluationRepository.delete(evaluation);
 
        Counter.builder("business.evaluations.deleted")
                .register(meterRegistry)
                .increment();
 
        log.info("Avaliação {} deletada pelo usuário {}", id, user.getEmail());
    }
 
    @Override
    @Cacheable(value = "dashboardSummary",
            key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().authentication.name")
    public DashboardSummaryDTO getDashboardSummary() {
 
        User user = getAuthenticatedUser();
 
        List<Evaluation> evaluations =
                evaluationRepository.findAllByUserId(user.getId());
 
        long total = evaluations.size();
 
        double average = evaluations.stream()
                .mapToInt(Evaluation::getScore)
                .average()
                .orElse(0.0);
 
        long excellent = evaluations.stream()
                .filter(e -> e.getClassification() == Classification.EXCELENTE)
                .count();
 
        long good = evaluations.stream()
                .filter(e -> e.getClassification() == Classification.BOM)
                .count();
 
        long regular = evaluations.stream()
                .filter(e -> e.getClassification() == Classification.REGULAR)
                .count();
 
        long bad = evaluations.stream()
                .filter(e -> e.getClassification() == Classification.RUIM)
                .count();
 
        Map<String, Long> byLanguage =
                evaluations.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getLanguage().name(),
                                Collectors.counting()
                        ));
 
        Map<LocalDate, Double> scoreEvolution =
                evaluations.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getCreatedAt().toLocalDate(),
                                Collectors.averagingInt(Evaluation::getScore)
                        ));
 
        double testsPercentage =
                total == 0 ? 0 :
                (evaluations.stream().filter(Evaluation::isHasTests).count() * 100.0) / total;
 
        double gitPercentage =
                total == 0 ? 0 :
                (evaluations.stream().filter(Evaluation::isUsesGit).count() * 100.0) / total;
 
        return new DashboardSummaryDTO(
                total,
                excellent,
                good,
                regular,
                bad,
                average,
                byLanguage,
                scoreEvolution,
                testsPercentage,
                gitPercentage
        );
    }
 
    // REGRAS DE NEGÓCIO
 
    private void validateFilter(EvaluationFilterDTO filter) {
 
        if (filter.getStartDate() != null &&
            filter.getEndDate() != null &&
            filter.getStartDate().isAfter(filter.getEndDate())) {
 
            throw new BusinessException(
                    "A data inicial não pode ser maior que a data final"
            );
        }
 
        if (filter.getMinScore() != null &&
            filter.getMaxScore() != null &&
            filter.getMinScore() > filter.getMaxScore()) {
 
            throw new BusinessException(
                    "O score mínimo não pode ser maior que o score máximo"
            );
        }
    }
 
    private int calculateScore(EvaluationRequestDTO dto) {
 
        // PILAR 1 — Boas práticas (40 pts)
        int boasPraticas = 0;
        if (Boolean.TRUE.equals(dto.getHasTests())) boasPraticas += 25;
        else boasPraticas -= 5;
        if (Boolean.TRUE.equals(dto.getUsesGit())) boasPraticas += 15;
        else boasPraticas -= 5;
 
        // PILAR 2 — Complexidade (30 pts)
        int complexidade = switch (dto.getComplexity()) {
            case 1 -> 30;
            case 2 -> 25;
            case 3 -> 15;
            case 4 -> 5;
            case 5 -> 0;
            default -> 10;
        };
 
        // PILAR 3 — Tamanho adequado (20 pts)
        int lines = dto.getLinesOfCode();
        int tamanho;
        if      (lines <= 50)   tamanho = 8;
        else if (lines <= 300)  tamanho = 20;
        else if (lines <= 1000) tamanho = 16;
        else if (lines <= 5000) tamanho = 10;
        else                    tamanho = 5;
 
        // PILAR 4 — Linguagem (10 pts)
        int linguagem = switch (dto.getLanguage()) {
            case JAVA, KOTLIN, CSHARP, TYPESCRIPT, RUST, GO -> 10;
            case PYTHON, SWIFT, CPP -> 8;
            case JAVASCRIPT, RUBY, DART -> 6;
            case PHP, C -> 5;
            case OTHER -> 4;
        };
 
        int total = boasPraticas + complexidade + tamanho + linguagem;
        return Math.max(0, Math.min(100, total));
    }
 
    private Classification classify(int score) {
        if (score >= 80) return Classification.EXCELENTE;
        if (score >= 60) return Classification.BOM;
        if (score >= 40) return Classification.REGULAR;
        return Classification.RUIM;
    }
 
    private EvaluationResponseDTO toResponseDTO(Evaluation evaluation) {
 
        log.info("🔄 Convertendo Evaluation ID={}", evaluation.getId());
 
        EvaluationResponseDTO dto = new EvaluationResponseDTO();
 
        dto.setId(evaluation.getId());
        dto.setProjectName(evaluation.getProjectName());
        dto.setLanguage(evaluation.getLanguage());
        dto.setScore(evaluation.getScore());
        dto.setAnalyzedBy(evaluation.getAnalyzedBy());
        dto.setCreatedAt(evaluation.getCreatedAt());
        dto.setHasTests(evaluation.isHasTests());
        dto.setUsesGit(evaluation.isUsesGit());
        dto.setLinesOfCode(evaluation.getLinesOfCode());
        dto.setComplexity(evaluation.getComplexity());
        dto.setDescription(evaluation.getDescription());
 
        Classification classification = evaluation.getClassification();
 
        if (classification == null) {
            log.warn("⚠️ Classification NULL → default REGULAR");
            dto.setClassification(Classification.REGULAR);
        } else {
            dto.setClassification(classification);
        }
 
        return dto;
    }
}