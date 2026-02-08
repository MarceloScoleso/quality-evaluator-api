package br.com.marceloscoleso.quality_evaluator_api.service;

import br.com.marceloscoleso.quality_evaluator_api.dto.ProjectDTO;

import java.util.List;

public interface ProjectService {

    ProjectDTO create(ProjectDTO dto);

    List<ProjectDTO> findAll();

    ProjectDTO findById(Long id);
}
