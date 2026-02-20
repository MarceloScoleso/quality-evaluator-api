package br.com.marceloscoleso.quality_evaluator_api.service.impl;

import br.com.marceloscoleso.quality_evaluator_api.dto.*;
import br.com.marceloscoleso.quality_evaluator_api.model.*;
import br.com.marceloscoleso.quality_evaluator_api.exception.*;
import br.com.marceloscoleso.quality_evaluator_api.repository.*;
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

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationServiceImpl.class);

    private final EvaluationRepository evaluationRepository;
    private final MeterRegistry meterRegistry;
    private final UserRepository userRepository;

    public EvaluationServiceImpl(
            EvaluationRepository evaluationRepository,
            MeterRegistry meterRegistry,
            UserRepository userRepository
    ) {
        this.evaluationRepository = evaluationRepository;
        this.meterRegistry = meterRegistry;
        this.userRepository = userRepository;
    }

    
    // USER AUTH
    

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    
    // CREATE
    

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

                    // 🔥 VINCULA AO USER
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

    
    // FIND ALL 
   

    @Override
    @Cacheable(value = "evaluations", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().authentication.name")
    public Page<EvaluationResponseDTO> findAll(Pageable pageable) {
        User user = getAuthenticatedUser();
        return evaluationRepository.findAllByUser(user, pageable)
                .map(this::toResponseDTO);
    }

   
    // FIND BY ID 
   

    @Override
    @Cacheable(value = "evaluation", key = "#id + '-' + T(org.springframework.security.core.context.SecurityContextHolder).getContext().authentication.name")
    public EvaluationResponseDTO findById(Long id) {

        User user = getAuthenticatedUser();

        return evaluationRepository.findByIdAndUser(id, user)
                .map(this::toResponseDTO)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Avaliação não encontrada"));
    }

    
    // FILTER 
    

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

    
    // EXPORT CSV 
   

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

  
    // STATS 
    

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

    
    // REGRAS DE NEGÓCIO
    

    private void validateFilter(EvaluationFilterDTO filter) {

    // Datas
    if (filter.getStartDate() != null &&
        filter.getEndDate() != null &&
        filter.getStartDate().isAfter(filter.getEndDate())) {

        throw new BusinessException(
                "A data inicial não pode ser maior que a data final"
        );
    }

    // Score
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

    // Peso por linguagem
    switch (dto.getLanguage()) {
        case JAVA -> score += 25;
        case CSHARP -> score += 22;
        case JAVASCRIPT -> score += 18;
        case TYPESCRIPT -> score += 18;
        case PYTHON -> score += 20;
        case KOTLIN -> score += 20;
        case GO -> score += 21;
        case PHP -> score += 15;
        case RUBY -> score += 15;
        case SWIFT -> score += 20;
        case C -> score += 22;
        case CPP -> score += 23;
        case RUST -> score += 24;
        case DART -> score += 17;
        case OTHER -> score += 10;
    }

    // Linhas de código
    int lines = dto.getLinesOfCode();

    if (lines <= 100) score += 10;
    else if (lines <= 500) score += 25;
    else if (lines <= 1000) score += 20;
    else score += 15;

    // Complexidade (1 a 5)
    int complexity = dto.getComplexity();

    switch (complexity) {
        case 1 -> score += 5;
        case 2 -> score += 10;
        case 3 -> score += 15;
        case 4 -> score += 10;
        case 5 -> score += 5;
    }

    // Testes automatizados
    if (Boolean.TRUE.equals(dto.getHasTests()))
        score += 20;

    // Uso de Git
    if (Boolean.TRUE.equals(dto.getUsesGit()))
        score += 10;

    // Critérios avançados de qualidade
    score += simulateCodeQuality(dto.getProjectName(), dto.getLanguage());

    // Limita score a 100
    return Math.min(score, 100);
}


// Simula fatores avançados de qualidade do projeto
private int simulateCodeQuality(String projectName, Language language) {

    int qualityScore = 0;

    // Nome do projeto como proxy para maturidade
    int nameLength = projectName.length();

    if (nameLength <= 10) qualityScore += 5;
    else if (nameLength <= 20) qualityScore += 10;
    else qualityScore += 15;

    // Ajuste por linguagem
    switch (language) {
        case JAVA, CSHARP, CPP, RUST -> qualityScore += 10;
        case PYTHON, JAVASCRIPT, TYPESCRIPT, KOTLIN, SWIFT, GO -> qualityScore += 7;
        default -> qualityScore += 5;
    }

    // Pequena variação randômica
    qualityScore += (int) (Math.random() * 10);

    return qualityScore;
}


// Classificação final baseada no score
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

    return dto;
}
}
