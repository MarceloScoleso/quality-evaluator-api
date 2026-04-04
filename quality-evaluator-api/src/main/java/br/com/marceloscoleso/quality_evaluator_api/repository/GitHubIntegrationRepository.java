package br.com.marceloscoleso.quality_evaluator_api.repository;
 
import br.com.marceloscoleso.quality_evaluator_api.model.GitHubIntegration;
import br.com.marceloscoleso.quality_evaluator_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.Optional;
 
public interface GitHubIntegrationRepository extends JpaRepository<GitHubIntegration, Long> {
 
    Optional<GitHubIntegration> findByUser(User user);
 
    boolean existsByUser(User user);
 
    void deleteByUser(User user);
}