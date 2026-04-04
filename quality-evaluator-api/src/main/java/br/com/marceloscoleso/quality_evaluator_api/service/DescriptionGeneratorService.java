package br.com.marceloscoleso.quality_evaluator_api.service;
 
import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.dto.RepoAnalysisData;
import br.com.marceloscoleso.quality_evaluator_api.model.Classification;
 
public interface DescriptionGeneratorService {
 
    /** Gera descrição a partir de dados manuais (modo formulário). */
    String generate(EvaluationRequestDTO dto, int score, Classification classification, String lang);
 
    /** Gera descrição rica a partir de análise real do repositório GitHub. */
    String generateFromRepoAnalysis(RepoAnalysisData data, String lang);
}