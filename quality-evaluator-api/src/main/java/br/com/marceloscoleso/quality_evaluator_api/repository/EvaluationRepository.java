package br.com.marceloscoleso.quality_evaluator_api.repository;

import br.com.marceloscoleso.quality_evaluator_api.model.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository
        extends JpaRepository<Evaluation, Long>,
                JpaSpecificationExecutor<Evaluation> {

    Page<Evaluation> findAllByUserId(Long userId, Pageable pageable);

    List<Evaluation> findAllByUserId(Long userId);

    Optional<Evaluation> findByIdAndUserId(Long id, Long userId);
}