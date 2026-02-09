package br.com.marceloscoleso.quality_evaluator_api.controller;

import br.com.marceloscoleso.quality_evaluator_api.dto.ProjectDTO;
import br.com.marceloscoleso.quality_evaluator_api.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // Criar projeto
    @PostMapping
    public ProjectDTO create(
            @RequestBody @Valid ProjectDTO dto
    ) {
        return projectService.create(dto);
    }

    // Listar projetos
    @GetMapping
    public Page<ProjectDTO> listAll(Pageable pageable) {
        return projectService.findAll(pageable);
    }

    // Buscar por ID
    @GetMapping("/{id}")
    public ProjectDTO findById(@PathVariable Long id) {
        return projectService.findById(id);
    }
}
