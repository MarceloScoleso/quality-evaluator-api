package br.com.marceloscoleso.quality_evaluator_api.controller;

import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationResponseDTO;
import br.com.marceloscoleso.quality_evaluator_api.service.EvaluationService;
import br.com.marceloscoleso.quality_evaluator_api.model.Classification;
import br.com.marceloscoleso.quality_evaluator_api.model.Language;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;


import java.util.List;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationFilterDTO;

@Tag(
        name = "Evaluations",
        description = "Endpoints para criação e consulta de avaliações de qualidade de projetos"
)
@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @Operation(
            summary = "Criar uma nova avaliação",
            description = """
            Cria uma avaliação de qualidade para um projeto.
            A pontuação e a classificação são calculadas automaticamente
            com base nos dados informados.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Avaliação criada com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EvaluationResponseDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationResponseDTO create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Dados do projeto a ser avaliado",
                    content = @Content(
                            schema = @Schema(implementation = EvaluationRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "Exemplo de avaliação",
                                    value = """
                                    {
                                      "projectName": "Quality Evaluator API",
                                      "language": "JAVA",
                                      "linesOfCode": 250,
                                      "complexity": 2,
                                      "hasTests": true,
                                      "usesGit": true,
                                      "analyzedBy": "Marcelo"
                                    }
                                    """
                            )
                    )
            )
            @RequestBody @Valid EvaluationRequestDTO request
    ) {
        return evaluationService.create(request);
    }

    @Operation(
            summary = "Listar avaliações",
            description = "Lista todas as avaliações com paginação"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de avaliações retornada com sucesso"
            )
    })
    @GetMapping
    public Page<EvaluationResponseDTO> findAll(
            @Parameter(description = "Número da página (começa em 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Quantidade de registros por página", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(
                    description = "Ordenação no formato campo,asc|desc",
                    example = "createdAt,desc"
            )
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortObj = Sort.by(
        sort.endsWith(",asc")
                ? Sort.Order.asc(java.util.Objects.requireNonNull(sort.replace(",asc", "")))
                : Sort.Order.desc(java.util.Objects.requireNonNull(sort.replace(",desc", "")))
    );

        Pageable pageable = PageRequest.of(page, size, sortObj);
        return evaluationService.findAll(pageable);
    }

    @Operation(
            summary = "Buscar avaliação por ID",
            description = "Retorna uma avaliação específica pelo seu ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Avaliação encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EvaluationResponseDTO.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
    })
    @GetMapping("/{id}")
    public EvaluationResponseDTO findById(
            @Parameter(description = "ID da avaliação", example = "1")
            @PathVariable Long id
    ) {
        return evaluationService.findById(id);
    }
    
    @Operation(
        summary = "Filtrar avaliações",
        description = """
        Permite filtrar avaliações utilizando os mesmos critérios
        disponíveis na versão console da aplicação.

        Filtros disponíveis:
        - Nome do projeto
        - Linguagem
        - Score mínimo e máximo
        - Classificação
        - Período de criação
        """
)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Lista de avaliações filtradas retornada com sucesso",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = Page.class)
                )
        )
})
@GetMapping("/filter")
public Page<EvaluationResponseDTO> filter(
        
        @Parameter(description = "Nome do projeto (parcial)", example = "quality")
        @RequestParam(required = false) String projectName,

        @Parameter(description = "Linguagem do projeto", example = "JAVA")
        @RequestParam(required = false) Language language,

        @Parameter(description = "Score mínimo", example = "60")
        @RequestParam(required = false) Integer minScore,

        @Parameter(description = "Score máximo", example = "90")
        @RequestParam(required = false) Integer maxScore,

        @Parameter(description = "Classificação", example = "BOM")
        @RequestParam(required = false) Classification classification,

        @Parameter(description = "Data inicial (yyyy-MM-dd)", example = "2024-01-01")
        @RequestParam(required = false) String startDate,

        @Parameter(description = "Data final (yyyy-MM-dd)", example = "2024-12-31")
        @RequestParam(required = false) String endDate,

        @RequestParam(defaultValue = "0") int page,

        @RequestParam(defaultValue = "6") int size,

        @RequestParam(defaultValue = "createdAt,desc") String sort
) {
        Sort sortObj = Sort.by(
        sort.endsWith(",asc")
                ? Sort.Order.asc(sort.replace(",asc", ""))
                : Sort.Order.desc(sort.replace(",desc", ""))
);

Pageable pageable = PageRequest.of(page, size, sortObj);

    EvaluationFilterDTO filter = new EvaluationFilterDTO();
    filter.setProjectName(projectName);
    filter.setClassification(classification);
    filter.setMinScore(minScore);
    filter.setMaxScore(maxScore);
    filter.setLanguage(language);

    if (startDate != null) {
        filter.setStartDate(java.time.LocalDate.parse(startDate));
    }

    if (endDate != null) {
        filter.setEndDate(java.time.LocalDate.parse(endDate));
    }

    
    

    return evaluationService.filter(filter, pageable);
}

@Operation(
        summary = "Exporta avaliações em CSV com filtros",
        description = """
        Permite exportar avaliações em CSV utilizando os mesmos critérios
        disponíveis na versão console da aplicação.

        Filtros disponíveis (todos opcionais):
        - Nome do projeto (parcial)
        - Linguagem do projeto
        - Score mínimo e máximo
        - Classificação
        - Período de criação (data inicial e final)

        Exemplo de uso:
        GET /api/evaluations/export/csv?projectName=quality&language=JAVA&minScore=60&maxScore=90&classification=BOM&startDate=2024-01-01&endDate=2024-12-31
        """
)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "CSV gerado com sucesso",
                content = @Content(mediaType = "text/csv")
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Filtros inválidos"
        ),
        @ApiResponse(
                responseCode = "404",
                description = "Nenhuma avaliação encontrada para exportação"
        )
})
@GetMapping(value = "/export/csv", produces = "text/csv")
public ResponseEntity<byte[]> exportCsv(
        @Parameter(description = "Nome do projeto (parcial)", example = "quality")
        @RequestParam(required = false) String projectName,

        @Parameter(description = "Linguagem do projeto", example = "JAVA")
        @RequestParam(required = false) Language language,

        @Parameter(description = "Score mínimo", example = "60")
        @RequestParam(required = false) Integer minScore,

        @Parameter(description = "Score máximo", example = "90")
        @RequestParam(required = false) Integer maxScore,

        @Parameter(description = "Classificação", example = "BOM")
        @RequestParam(required = false) Classification classification,

        @Parameter(description = "Data inicial (yyyy-MM-dd)", example = "2024-01-01")
        @RequestParam(required = false) String startDate,

        @Parameter(description = "Data final (yyyy-MM-dd)", example = "2024-12-31")
        @RequestParam(required = false) String endDate
) {

    EvaluationFilterDTO filter = new EvaluationFilterDTO();
    filter.setProjectName(projectName);
    filter.setLanguage(language);
    filter.setMinScore(minScore);
    filter.setMaxScore(maxScore);
    filter.setClassification(classification);

    if (startDate != null) {
        filter.setStartDate(java.time.LocalDate.parse(startDate));
    }

    if (endDate != null) {
        filter.setEndDate(java.time.LocalDate.parse(endDate));
    }

    byte[] csv = evaluationService.exportCsv(filter);

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=evaluations.csv")
            .contentType(new MediaType("text", "csv"))
            .body(csv);
}
}
