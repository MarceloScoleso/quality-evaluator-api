package br.com.marceloscoleso.quality_evaluator_api.repository;

import br.com.marceloscoleso.quality_evaluator_api.model.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository
        extends JpaRepository<Evaluation, Long>,
                JpaSpecificationExecutor<Evaluation> {

    @Query(
    value = "SELECT e FROM Evaluation e JOIN FETCH e.user WHERE e.user.id = :userId",
    countQuery = "SELECT COUNT(e) FROM Evaluation e WHERE e.user.id = :userId"
)
Page<Evaluation> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT e FROM Evaluation e JOIN FETCH e.user WHERE e.user.id = :userId")
List<Evaluation> findAllByUserId(@Param("userId") Long userId);

        @Query("SELECT e FROM Evaluation e JOIN FETCH e.user WHERE e.id = :id AND e.user.id = :userId")
        Optional<Evaluation> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}