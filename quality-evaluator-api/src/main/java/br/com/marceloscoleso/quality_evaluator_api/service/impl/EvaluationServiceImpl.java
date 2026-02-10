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
                    String classification = classify(score);

                    Evaluation evaluation = new Evaluation();
                    evaluation.setProjectName(dto.getProjectName());
                    evaluation.setLanguage(dto.getLanguage());
                    evaluation.setScore(score);
                    evaluation.setClassification(classification);
                    evaluation.setAnalyzedBy(dto.getAnalyzedBy());
                    evaluation.setCreatedAt(LocalDateTime.now());

                    Counter.builder("business.evaluations.created")
                            .tag("language", dto.getLanguage().name())
                            .tag("classification", classification)
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

        switch (dto.getLanguage()) {
            case JAVA -> score += 30;
            default -> score += 20;
        }

        int lines = dto.getLinesOfCode();
        if (lines <= 200) score += 30;
        else if (lines <= 500) score += 20;
        else score += 10;

        int complexity = dto.getComplexity();
        if (complexity <= 2) score += 20;
        else if (complexity == 3) score += 10;
        else score += 5;

        if (Boolean.TRUE.equals(dto.getHasTests())) score += 10;
        if (Boolean.TRUE.equals(dto.getUsesGit())) score += 10;

        return Math.min(score, 100);
    }

    private String classify(int score) {
        if (score >= 85) return "EXCELENTE";
        if (score >= 70) return "BOM";
        if (score >= 50) return "REGULAR";
        return "RUIM";
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
