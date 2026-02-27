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
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
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

                    int score = calculateScore(dto);
                    Classification classification = classify(score);

                    Evaluation evaluation = new Evaluation();
                    evaluation.setProjectName(dto.getProjectName());
                    evaluation.setLanguage(dto.getLanguage());
                    evaluation.setScore(score);
                    evaluation.setClassification(classification.name());
                    evaluation.setAnalyzedBy(dto.getAnalyzedBy());
                    evaluation.setCreatedAt(LocalDateTime.now());
                    evaluation.setHasTests(dto.getHasTests());
                    evaluation.setUsesGit(dto.getUsesGit());
                    evaluation.setLinesOfCode(dto.getLinesOfCode());
                    evaluation.setComplexity(dto.getComplexity());

                    if (dto.getDescription() != null && !dto.getDescription().trim().isEmpty()) {
                        evaluation.setDescription(dto.getDescription());
                    } else {
                        evaluation.setDescription(
                            descriptionGeneratorService.generate(dto, score, classification)
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
    @Cacheable(value = "evaluations", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().authentication.name")
    public Page<EvaluationResponseDTO> findAll(Pageable pageable) {
        User user = getAuthenticatedUser();
        return evaluationRepository.findAllByUser(user, pageable)
                .map(this::toResponseDTO);
    }
   

    @Override
    @Cacheable(value = "evaluation", key = "#id + '-' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().authentication.name")
    public EvaluationResponseDTO findById(Long id) {

        User user = getAuthenticatedUser();

        return evaluationRepository.findByIdAndUser(id, user)
                .map(this::toResponseDTO)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Avaliação não encontrada"));
    }
    

    @Override
    public Page<EvaluationResponseDTO> filter(EvaluationFilterDTO filter, Pageable pageable) {

        validateFilter(filter);

        User user = getAuthenticatedUser();

        List<EvaluationResponseDTO> filtered = evaluationRepository.findAllByUser(user).stream()

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
                                e.getClassification().equalsIgnoreCase(filter.getClassification().name())
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
                evaluationRepository.findAllByUser(user);

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
                evaluationRepository.findAllByUser(user);

        long total = evaluations.size();

        double average = evaluations.stream()
                .mapToInt(Evaluation::getScore)
                .average()
                .orElse(0.0);

        long excellentCount = evaluations.stream()
                .filter(e -> "EXCELENTE".equalsIgnoreCase(e.getClassification()))
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

    Evaluation evaluation = evaluationRepository
            .findByIdAndUser(id, user)
            .orElseThrow(() ->
                    new ResourceNotFoundException("Avaliação não encontrada"));

    int score = calculateScore(dto);
    Classification classification = classify(score);

    evaluation.setProjectName(dto.getProjectName());
    evaluation.setLanguage(dto.getLanguage());
    evaluation.setLinesOfCode(dto.getLinesOfCode());
    evaluation.setComplexity(dto.getComplexity());
    evaluation.setHasTests(dto.getHasTests());
    evaluation.setUsesGit(dto.getUsesGit());
    evaluation.setAnalyzedBy(dto.getAnalyzedBy());
    evaluation.setScore(score);
    evaluation.setClassification(classification.name());

    if (dto.getDescription() != null && !dto.getDescription().trim().isEmpty()) {
        evaluation.setDescription(dto.getDescription());
    } else {
        evaluation.setDescription(
                descriptionGeneratorService.generate(dto, score, classification)
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

    Evaluation evaluation = evaluationRepository
            .findByIdAndUser(id, user)
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
            evaluationRepository.findAllByUser(user);

    long total = evaluations.size();

    double average = evaluations.stream()
            .mapToInt(Evaluation::getScore)
            .average()
            .orElse(0.0);

    long excellent = evaluations.stream()
            .filter(e -> "EXCELENTE".equalsIgnoreCase(e.getClassification()))
            .count();

    long good = evaluations.stream()
            .filter(e -> "BOM".equalsIgnoreCase(e.getClassification()))
            .count();

    long regular = evaluations.stream()
            .filter(e -> "REGULAR".equalsIgnoreCase(e.getClassification()))
            .count();

    long bad = evaluations.stream()
            .filter(e -> "RUIM".equalsIgnoreCase(e.getClassification()))
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

    int score = 0;

  
    switch (dto.getLanguage()) {
        case JAVA, CSHARP, RUST -> score += 18;
        case CPP, GO, KOTLIN -> score += 16;
        case PYTHON, TYPESCRIPT, SWIFT -> score += 14;
        case JAVASCRIPT -> score += 12;
        case C -> score += 13;
        case PHP, RUBY, DART -> score += 10;
        case OTHER -> score += 8;
    }

   
    int lines = dto.getLinesOfCode();

    if (lines <= 100) score += 5;
    else if (lines <= 500) score += 18;
    else if (lines <= 1000) score += 15;
    else if (lines <= 5000) score += 10;
    else score += 5;

  
    switch (dto.getComplexity()) {
        case 1 -> score += 20;
        case 2 -> score += 15;
        case 3 -> score += 8;
        case 4 -> score -= 10;
        case 5 -> score -= 20;
    }

  
    if (Boolean.TRUE.equals(dto.getHasTests()))
        score += 20;
    else
        score -= 10;

   
    if (Boolean.TRUE.equals(dto.getUsesGit()))
        score += 8;
    else
        score -= 5;

    
    score += simulateCodeQuality(dto.getProjectName(), dto.getLanguage());

    
    if (score > 100) return 100;
    if (score < 0) return 0;

    return score;
}

private int simulateCodeQuality(String projectName, Language language) {

    int qualityScore = 0;

    int nameLength = projectName.length();

    if (nameLength <= 10) qualityScore += 3;
    else if (nameLength <= 20) qualityScore += 6;
    else qualityScore += 8;

    switch (language) {
        case JAVA, CSHARP, RUST -> qualityScore += 6;
        case CPP, GO -> qualityScore += 5;
        case PYTHON, TYPESCRIPT, KOTLIN, SWIFT -> qualityScore += 4;
        default -> qualityScore += 3;
    }

    int variation = Math.abs(projectName.hashCode() % 5);
    qualityScore += variation;

    return qualityScore;
}

private Classification classify(int score) {

    if (score >= 85) return Classification.EXCELENTE;
    if (score >= 70) return Classification.BOM;
    if (score >= 50) return Classification.REGULAR;

    return Classification.RUIM;
}


private EvaluationResponseDTO toResponseDTO(Evaluation evaluation) {

    EvaluationResponseDTO dto = new EvaluationResponseDTO();

    dto.setId(evaluation.getId());
    dto.setProjectName(evaluation.getProjectName());
    dto.setLanguage(evaluation.getLanguage());
    dto.setScore(evaluation.getScore());
    dto.setClassification(
            Classification.valueOf(evaluation.getClassification())
    );
    dto.setAnalyzedBy(evaluation.getAnalyzedBy());
    dto.setCreatedAt(evaluation.getCreatedAt());

    dto.setHasTests(evaluation.isHasTests());
    dto.setUsesGit(evaluation.isUsesGit());
    
    dto.setLinesOfCode(evaluation.getLinesOfCode());
    dto.setComplexity(evaluation.getComplexity());
    dto.setDescription(evaluation.getDescription());
    return dto;
}
}
