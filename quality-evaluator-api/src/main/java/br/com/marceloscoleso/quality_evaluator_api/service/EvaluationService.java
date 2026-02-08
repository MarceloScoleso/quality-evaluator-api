package br.com.marceloscoleso.quality_evaluator_api.service;

import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationResponseDTO;

import java.util.List;

public interface EvaluationService {

    EvaluationResponseDTO create(EvaluationRequestDTO dto);

    List<EvaluationResponseDTO> findAll();

    EvaluationResponseDTO findById(Long id);
}
