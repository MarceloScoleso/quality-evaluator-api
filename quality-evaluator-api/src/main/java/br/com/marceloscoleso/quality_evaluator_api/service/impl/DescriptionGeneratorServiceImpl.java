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
    private final String model;

    public DescriptionGeneratorServiceImpl(
            @Value("${groq.api-key}") String apiKey,
            @Value("${groq.model:llama-3.3-70b-versatile}") String model
    ) {
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String generate(EvaluationRequestDTO dto, int score, Classification classification) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 300,
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "Você é um especialista em qualidade de software. " +
                                    "Gere descrições técnicas objetivas e profissionais em português, " +
                                    "com no máximo 3 frases. Seja direto e evite introduções genéricas."),
                            Map.of("role", "user", "content", buildPrompt(dto, score, classification))
                    )
            );

            Map response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map> choices = (List<Map>) response.get("choices");
            Map message = (Map) choices.get(0).get("message");
            String text = (String) message.get("content");

            log.info("✅ Descrição gerada pelo Groq para projeto: {}", dto.getProjectName());
            return text.trim();

        } catch (Exception e) {
            log.error("❌ Falha ao chamar Groq, usando fallback. Erro: {}", e.getMessage());
            return generateFallback(dto, score, classification);
        }
    }

    private String buildPrompt(EvaluationRequestDTO dto, int score, Classification classification) {
        return """
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