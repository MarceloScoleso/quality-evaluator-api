package br.com.marceloscoleso.quality_evaluator_api.service.impl;

import br.com.marceloscoleso.quality_evaluator_api.dto.ProjectDTO;
import br.com.marceloscoleso.quality_evaluator_api.exception.ResourceNotFoundException;
import br.com.marceloscoleso.quality_evaluator_api.model.Project;
import br.com.marceloscoleso.quality_evaluator_api.repository.ProjectRepository;
import br.com.marceloscoleso.quality_evaluator_api.service.ProjectService;
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
import java.util.UUID;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final MeterRegistry meterRegistry;
    private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);

    public ProjectServiceImpl(ProjectRepository projectRepository, MeterRegistry meterRegistry) {
        this.projectRepository = projectRepository;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @CacheEvict(value = "projects", allEntries = true)
    public ProjectDTO create(ProjectDTO dto) {

        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        log.info("Iniciando criação de projeto", Map.of(
                "requestId", requestId,
                "projectName", dto.getName()
        ));

        ProjectDTO result = Timer.builder("business.projects.create.time")
                .description("Tempo para criar um projeto")
                .register(meterRegistry)
                .record(() -> {
                    Project project = new Project();
                    project.setName(dto.getName());

                    Counter.builder("business.projects.created")
                            .register(meterRegistry)
                            .increment();

                    Project saved = projectRepository.save(project);

                    log.info("Projeto criado com sucesso", Map.of(
                            "requestId", requestId,
                            "projectId", saved.getId()
                    ));

                    return toDTO(saved);
                });

        MDC.remove("requestId");
        return result;
    }

    @Override
    @Cacheable("projects")
    public Page<ProjectDTO> findAll(Pageable pageable) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        log.info("Listando projetos", Map.of("requestId", requestId));

        Counter.builder("business.projects.listed")
                .register(meterRegistry)
                .increment();

        Page<ProjectDTO> result = projectRepository.findAll(pageable)
                .map(this::toDTO);

        MDC.remove("requestId");
        return result;
    }

    @Override
    @Cacheable(value = "project", key = "#id")
    public ProjectDTO findById(Long id) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        log.info("Buscando projeto por ID", Map.of(
                "requestId", requestId,
                "projectId", id
        ));

        ProjectDTO result = projectRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> {
                    Counter.builder("business.projects.not_found")
                            .register(meterRegistry)
                            .increment();

                    log.warn("Projeto não encontrado", Map.of(
                            "requestId", requestId,
                            "projectId", id
                    ));

                    return new ResourceNotFoundException("Projeto não encontrado");
                });

        MDC.remove("requestId");
        return result;
    }

    private ProjectDTO toDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        return dto;
    }
}
