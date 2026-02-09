package br.com.marceloscoleso.quality_evaluator_api.service;

import br.com.marceloscoleso.quality_evaluator_api.dto.ProjectDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {

    ProjectDTO create(ProjectDTO dto);

    Page<ProjectDTO> findAll(Pageable pageable);

    ProjectDTO findById(Long id);
}
