package br.com.marceloscoleso.quality_evaluator_api.service.impl;

import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationResponseDTO;
import br.com.marceloscoleso.quality_evaluator_api.exception.ResourceNotFoundException;
import br.com.marceloscoleso.quality_evaluator_api.model.Evaluation;
import br.com.marceloscoleso.quality_evaluator_api.repository.EvaluationRepository;
import br.com.marceloscoleso.quality_evaluator_api.service.EvaluationService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.Map;
import java.time.LocalDateTime;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final MeterRegistry meterRegistry;
    private static final Logger log = LoggerFactory.getLogger(EvaluationServiceImpl.class);
    
    public EvaluationServiceImpl(
            EvaluationRepository evaluationRepository,
            MeterRegistry meterRegistry
    ) {
        this.evaluationRepository = evaluationRepository;
        this.meterRegistry = meterRegistry;
    }

    @Override
@CacheEvict(value = "evaluations", allEntries = true)
public EvaluationResponseDTO create(EvaluationRequestDTO dto) {

    String requestId = MDC.get("requestId"); 
    log.info("Iniciando criação de avaliação", Map.of(
            "requestId", requestId,
            "projectName", dto.getProjectName(),
            "language", dto.getLanguage(),
            "classification", dto.getClassification()
    ));

    return Timer.builder("business.evaluations.create.time")
            .description("Tempo para criar uma avaliação")
            .register(meterRegistry)
            .record(() -> {

                Evaluation evaluation = new Evaluation();
                evaluation.setProjectName(dto.getProjectName());
                evaluation.setLanguage(dto.getLanguage());
                evaluation.setScore(dto.getScore());
                evaluation.setClassification(dto.getClassification());
                evaluation.setAnalyzedBy(dto.getAnalyzedBy());
                evaluation.setCreatedAt(LocalDateTime.now());

                Counter.builder("business.evaluations.created")
                        .tag("language", dto.getLanguage().name())
                        .tag("classification", dto.getClassification())
                        .register(meterRegistry)
                        .increment();

                Evaluation saved = evaluationRepository.save(evaluation);

                log.info("Avaliação criada com sucesso", Map.of(
                        "requestId", requestId,
                        "evaluationId", saved.getId()
                ));

                return toResponseDTO(saved);
            });
}

    @Override
    @Cacheable("evaluations")
    public Page<EvaluationResponseDTO> findAll(Pageable pageable) {
        return evaluationRepository.findAll(pageable)
                .map(this::toResponseDTO);
    }

    @Override
@Cacheable(value = "evaluation", key = "#id")
public EvaluationResponseDTO findById(Long id) {

    String requestId = MDC.get("requestId");

    return evaluationRepository.findById(id)
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

    private EvaluationResponseDTO toResponseDTO(Evaluation evaluation) {
        EvaluationResponseDTO dto = new EvaluationResponseDTO();
        dto.setId(evaluation.getId());
        dto.setProjectName(evaluation.getProjectName());
        dto.setLanguage(evaluation.getLanguage());
        dto.setScore(evaluation.getScore());
        dto.setClassification(evaluation.getClassification());
        dto.setAnalyzedBy(evaluation.getAnalyzedBy());
        dto.setCreatedAt(evaluation.getCreatedAt());
        return dto;
    }
}
