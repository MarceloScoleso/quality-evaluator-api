package br.com.marceloscoleso.quality_evaluator_api.service;

import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.model.Classification;

public interface DescriptionGeneratorService {
    String generate(EvaluationRequestDTO dto, int score, Classification classification);
}