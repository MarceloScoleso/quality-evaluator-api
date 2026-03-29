package br.com.marceloscoleso.quality_evaluator_api.service.impl;

import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.model.Classification;
import br.com.marceloscoleso.quality_evaluator_api.service.DescriptionGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class DescriptionGeneratorServiceImpl implements DescriptionGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(DescriptionGeneratorServiceImpl.class);

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public DescriptionGeneratorServiceImpl(
            @Value("${spring.gemini.api-key}") String apiKey,
            @Value("${spring.gemini.model}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String generate(EvaluationRequestDTO dto, int score, Classification classification) {
        try {
            String prompt = buildPrompt(dto, score, classification);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "maxOutputTokens", 300,
                            "temperature", 0.7
                    )
            );

            Map response = webClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map> candidates = (List<Map>) response.get("candidates");
            Map content = (Map) candidates.get(0).get("content");
            List<Map> parts = (List<Map>) content.get("parts");
            String text = (String) parts.get(0).get("text");

            log.info("✅ Descrição gerada pelo Gemini para projeto: {}", dto.getProjectName());
            return text.trim();

        } catch (Exception e) {
            log.error("❌ Falha ao chamar Gemini, usando fallback. Erro: {}", e.getMessage());
            return generateFallback(dto, score, classification);
        }
    }

    private String buildPrompt(EvaluationRequestDTO dto, int score, Classification classification) {
        return """
                Você é um especialista em qualidade de software.
                Avalie o projeto com as seguintes características:
                - Nome: %s
                - Linguagem: %s
                - Linhas de código: %d
                - Complexidade: %d/5
                - Possui testes automatizados: %s
                - Utiliza Git: %s
                - Score final: %d/100
                - Classificação: %s

                Gere uma descrição técnica profissional em português destacando os pontos \
                fortes, fracos e uma recomendação principal de melhoria. \
                Máximo 3 frases. Seja direto e objetivo, sem introduções genéricas.
                """.formatted(
                dto.getProjectName(),
                dto.getLanguage().name(),
                dto.getLinesOfCode(),
                dto.getComplexity(),
                Boolean.TRUE.equals(dto.getHasTests()) ? "Sim" : "Não",
                Boolean.TRUE.equals(dto.getUsesGit()) ? "Sim" : "Não",
                score,
                classification.name()
        );
    }

    private String generateFallback(EvaluationRequestDTO dto, int score, Classification classification) {
        String qualidade = switch (classification) {
            case EXCELENTE -> "excelente qualidade técnica";
            case BOM -> "boa qualidade técnica";
            case REGULAR -> "qualidade técnica regular";
            case RUIM -> "baixa qualidade técnica";
        };

        String testes = Boolean.TRUE.equals(dto.getHasTests())
                ? "A presença de testes automatizados é um ponto positivo relevante."
                : "A ausência de testes automatizados é o principal ponto de melhoria.";

        return "O projeto %s em %s obteve score %d/100, demonstrando %s. %s"
                .formatted(dto.getProjectName(), dto.getLanguage().name(), score, qualidade, testes);
    }
}