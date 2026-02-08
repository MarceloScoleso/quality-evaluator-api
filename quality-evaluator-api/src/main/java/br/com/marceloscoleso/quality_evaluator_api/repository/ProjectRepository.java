package br.com.marceloscoleso.quality_evaluator_api.repository;

import br.com.marceloscoleso.quality_evaluator_api.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByNameIgnoreCase(String name);
}