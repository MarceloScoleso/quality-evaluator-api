package br.com.marceloscoleso.quality_evaluator_api.service.impl;

import br.com.marceloscoleso.quality_evaluator_api.dto.ProjectDTO;
import br.com.marceloscoleso.quality_evaluator_api.model.Project;
import br.com.marceloscoleso.quality_evaluator_api.repository.ProjectRepository;
import br.com.marceloscoleso.quality_evaluator_api.service.ProjectService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public ProjectDTO create(ProjectDTO dto) {
        Project project = new Project();
        project.setName(dto.getName());

        Project saved = projectRepository.save(project);
        return toDTO(saved);
    }

    @Override
    public List<ProjectDTO> findAll() {
        return projectRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectDTO findById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        return toDTO(project);
    }

    private ProjectDTO toDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        return dto;
    }
}
