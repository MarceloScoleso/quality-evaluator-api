package br.com.marceloscoleso.quality_evaluator_api.repository;

import br.com.marceloscoleso.quality_evaluator_api.model.Evaluation;
import br.com.marceloscoleso.quality_evaluator_api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository
        extends JpaRepository<Evaluation, Long>,
                JpaSpecificationExecutor<Evaluation> {

    Page<Evaluation> findAllByUser(User user, Pageable pageable);

    List<Evaluation> findAllByUser(User user);

    Optional<Evaluation> findByIdAndUser(Long id, User user);
}
