package br.com.marceloscoleso.quality_evaluator_api.service;

import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationResponseDTO;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EvaluationService {

    EvaluationResponseDTO create(EvaluationRequestDTO dto);

    Page<EvaluationResponseDTO> findAll(Pageable pageable);

    EvaluationResponseDTO findById(Long id);
}
