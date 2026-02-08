package br.com.marceloscoleso.quality_evaluator_api.controller;

import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationResponseDTO;
import br.com.marceloscoleso.quality_evaluator_api.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    // Criar avaliação
    @PostMapping
    public EvaluationResponseDTO create(
            @RequestBody @Valid EvaluationRequestDTO request
    ) {
        return evaluationService.create(request);
    }

    // Listar histórico completo
    @GetMapping
    public List<EvaluationResponseDTO> findAll() {
        return evaluationService.findAll();
    }

    // Buscar por ID
    @GetMapping("/{id}")
    public EvaluationResponseDTO findById(@PathVariable Long id) {
        return evaluationService.findById(id);
    }
}
