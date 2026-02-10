package br.com.marceloscoleso.quality_evaluator_api.service.impl;

import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationFilterDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationResponseDTO;
import br.com.marceloscoleso.quality_evaluator_api.model.Classification;
import br.com.marceloscoleso.quality_evaluator_api.exception.BusinessException;
import br.com.marceloscoleso.quality_evaluator_api.exception.ResourceNotFoundException;
import br.com.marceloscoleso.quality_evaluator_api.model.Evaluation;
import br.com.marceloscoleso.quality_evaluator_api.repository.EvaluationRepository;
import br.com.marceloscoleso.quality_evaluator_api.service.EvaluationService;
import br.com.marceloscoleso.quality_evaluator_api.util.CsvExporterApi;
import br.com.marceloscoleso.quality_evaluator_api.exception.InvalidLanguageException;
import br.com.marceloscoleso.quality_evaluator_api.model.Language;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationServiceImpl.class);

    private final EvaluationRepository evaluationRepository;
    private final MeterRegistry meterRegistry;

    public EvaluationServiceImpl(
            EvaluationRepository evaluationRepository,
            MeterRegistry meterRegistry
    ) {
        this.evaluationRepository = evaluationRepository;
        this.meterRegistry = meterRegistry;
    }

    // CREATE
       

    @Override
    @CacheEvict(value = "evaluations", allEntries = true)
    public EvaluationResponseDTO create(EvaluationRequestDTO dto) {

        String requestId = MDC.get("requestId");

        log.info("Iniciando avaliação de projeto", Map.of(
                "requestId", requestId,
                "projectName", dto.getProjectName(),
                "language", dto.getLanguage()
        ));

        if (dto.getLanguage() == null || !Arrays.asList(Language.values()).contains(dto.getLanguage())) {
        String allowed = String.join(", ", Arrays.stream(Language.values())
                                .map(Language::name)
                                .toArray(String[]::new));
        throw new InvalidLanguageException(
        "Linguagem inválida. Linguagens aceitas: " + allowed
        );
        }
        
        return Timer.builder("business.evaluations.create.time")
                .description("Tempo para criar uma avaliação")
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

                    Counter.builder("business.evaluations.created")
                            .tag("language", dto.getLanguage().name())
                            .tag("classification", classification.name())
                            .register(meterRegistry)
                            .increment();

                    Evaluation saved = evaluationRepository.save(evaluation);

                    log.info("Avaliação criada com sucesso", Map.of(
                            "requestId", requestId,
                            "evaluationId", saved.getId(),
                            "score", score,
                            "classification", classification
                    ));

                    return toResponseDTO(saved);
                });
                
    }

    // FIND ALL
       

    @Override
    @Cacheable("evaluations")
    public Page<EvaluationResponseDTO> findAll(Pageable pageable) {
        return evaluationRepository.findAll(java.util.Objects.requireNonNull(pageable))
        .map(this::toResponseDTO);
    }

    // FIND BY ID
     

    @Override
    @Cacheable(value = "evaluation", key = "#id")
    public EvaluationResponseDTO findById(Long id) {

        String requestId = MDC.get("requestId");

        return evaluationRepository.findById(java.util.Objects.requireNonNull(id))
        .map(this::toResponseDTO)
        .orElseThrow(() -> {
                    Counter.builder("business.evaluations.not_found")
                            .register(meterRegistry)
                            .increment();

                    log.warn("Avaliação não encontrada", Map.of(
                            "requestId", requestId,
                            "evaluationId", id
                    ));

                    return new ResourceNotFoundException("Avaliação não encontrada");
                });
    }

    // FILTER 

    @Override
    public List<EvaluationResponseDTO> filter(EvaluationFilterDTO filter) {

    validateFilter(filter);

    return evaluationRepository.findAll().stream()

            // Data
            .filter(e -> {
                LocalDate date = e.getCreatedAt().toLocalDate();
                if (filter.getStartDate() != null && date.isBefore(filter.getStartDate())) return false;
                if (filter.getEndDate() != null && date.isAfter(filter.getEndDate())) return false;
                return true;
            })

            // Nome do projeto
            .filter(e ->
                    filter.getProjectName() == null ||
                    e.getProjectName().toLowerCase()
                            .contains(filter.getProjectName().toLowerCase())
            )

            // Linguagem
            .filter(e ->
                    filter.getLanguage() == null ||
                    e.getLanguage() == filter.getLanguage()
            )

            // Score
            .filter(e -> {
                if (filter.getMinScore() != null && e.getScore() < filter.getMinScore()) return false;
                if (filter.getMaxScore() != null && e.getScore() > filter.getMaxScore()) return false;
                return true;
            })

            // Classificação
            .filter(e ->
                filter.getClassification() == null ||
                e.getClassification().equalsIgnoreCase(filter.getClassification().name())
            )

            // Ordenação
            .sorted(Comparator.comparing(Evaluation::getCreatedAt).reversed())

            
            .map(this::toResponseDTO)

            .toList();
}

    @Override
    public byte[] exportCsv(EvaluationFilterDTO filter) {

    validateFilter(filter);

    List<Evaluation> evaluations = evaluationRepository.findAll().stream()

            .filter(e -> {
                if (filter.getStartDate() != null &&
                        e.getCreatedAt().toLocalDate().isBefore(filter.getStartDate())) {
                    return false;
                }
                if (filter.getEndDate() != null &&
                        e.getCreatedAt().toLocalDate().isAfter(filter.getEndDate())) {
                    return false;
                }
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
            .toList();

    if (evaluations.isEmpty()) {
        throw new BusinessException("Nenhuma avaliação encontrada para exportação");
    }

    return CsvExporterApi.export(evaluations);
}
    // REGRAS DE NEGÓCIO
       

    private void validateFilter(EvaluationFilterDTO filter) {

    // Datas
    if (filter.getStartDate() != null
            && filter.getEndDate() != null
            && filter.getStartDate().isAfter(filter.getEndDate())) {

        throw new BusinessException(
                "A data inicial não pode ser maior que a data final"
        );
    }

    // Score
    if (filter.getMinScore() != null && filter.getMaxScore() != null
            && filter.getMinScore() > filter.getMaxScore()) {

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
    if (Boolean.TRUE.equals(dto.getHasTests())) score += 20;

    // Uso de Git
    if (Boolean.TRUE.equals(dto.getUsesGit())) score += 10;

    // Critérios avançados de qualidade (modularidade, documentação, legibilidade)
    score += simulateCodeQuality(dto.getProjectName(), dto.getLanguage());

    // Limita score a 100
    return Math.min(score, 100);
}


    // Simula fatores avançados de qualidade do projeto

    private int simulateCodeQuality(String projectName, Language language) {
    int qualityScore = 0;

    // Nome do projeto como proxy para maturidade do design
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

    // Pequena variação randômica para simular qualidade real do código
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
