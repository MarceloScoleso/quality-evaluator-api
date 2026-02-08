package br.com.marceloscoleso.quality_evaluator_api.repository;

import br.com.marceloscoleso.quality_evaluator_api.model.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationRepository
        extends JpaRepository<Evaluation, Long>,
                JpaSpecificationExecutor<Evaluation> {
}