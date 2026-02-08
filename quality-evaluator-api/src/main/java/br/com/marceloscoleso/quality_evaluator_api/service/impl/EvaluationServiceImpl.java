package br.com.marceloscoleso.quality_evaluator_api.service.impl;

import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationResponseDTO;
import br.com.marceloscoleso.quality_evaluator_api.model.Evaluation;
import br.com.marceloscoleso.quality_evaluator_api.repository.EvaluationRepository;
import br.com.marceloscoleso.quality_evaluator_api.service.EvaluationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;

    public EvaluationServiceImpl(EvaluationRepository evaluationRepository) {
        this.evaluationRepository = evaluationRepository;
    }

    @Override
    public EvaluationResponseDTO create(EvaluationRequestDTO dto) {
        Evaluation evaluation = new Evaluation();
        evaluation.setProjectName(dto.getProjectName());
        evaluation.setLanguage(dto.getLanguage());
        evaluation.setScore(dto.getScore());
        evaluation.setClassification(dto.getClassification());
        evaluation.setAnalyzedBy(dto.getAnalyzedBy());
        evaluation.setCreatedAt(LocalDateTime.now());

        Evaluation saved = evaluationRepository.save(evaluation);
        return toResponseDTO(saved);
    }

    @Override
    public List<EvaluationResponseDTO> findAll() {
        return evaluationRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EvaluationResponseDTO findById(Long id) {
        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avaliação não encontrada"));

        return toResponseDTO(evaluation);
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
