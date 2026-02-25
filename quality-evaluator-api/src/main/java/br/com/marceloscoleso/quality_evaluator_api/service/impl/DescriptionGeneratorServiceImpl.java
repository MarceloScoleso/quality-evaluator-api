package br.com.marceloscoleso.quality_evaluator_api.service.impl;

import br.com.marceloscoleso.quality_evaluator_api.dto.EvaluationRequestDTO;
import br.com.marceloscoleso.quality_evaluator_api.model.Classification;
import br.com.marceloscoleso.quality_evaluator_api.service.DescriptionGeneratorService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class DescriptionGeneratorServiceImpl implements DescriptionGeneratorService {

    private final Random random = new Random();

    private String pick(List<String> options) {
        return options.get(random.nextInt(options.size()));
    }

    @Override
    public String generate(EvaluationRequestDTO dto, int score, Classification classification) {

        StringBuilder description = new StringBuilder();

        description.append(buildIntro(dto));
        description.append(buildTechnicalContext(dto));
        description.append(buildQualityAnalysis(dto, classification));
        description.append(buildFinalVerdict(score, classification));

        return description.toString().trim();
    }

    
    private String buildIntro(EvaluationRequestDTO dto) {

        List<String> intros = List.of(
                "O projeto \"%s\" desenvolvido em %s demonstra características técnicas interessantes. ",
                "A aplicação \"%s\", construída com %s, apresenta uma proposta estrutural relevante. ",
                "O sistema \"%s\" implementado utilizando %s revela decisões arquiteturais específicas. ",
                "Analisando o projeto \"%s\" em %s, observam-se aspectos técnicos distintos. "
        );

        return pick(intros).formatted(
                dto.getProjectName(),
                dto.getLanguage()
        );
    }

    
    private String buildTechnicalContext(EvaluationRequestDTO dto) {

        StringBuilder context = new StringBuilder();

        context.append(
                pick(List.of(
                        "Com %d linhas de código e complexidade %d, ",
                        "Totalizando %d linhas e nível de complexidade %d, ",
                        "Estruturado em %d linhas com complexidade %d, "
                )).formatted(dto.getLinesOfCode(), dto.getComplexity())
        );

        if (Boolean.TRUE.equals(dto.getHasTests())) {
            context.append(
                    pick(List.of(
                            "conta com cobertura de testes automatizados, ",
                            "inclui validação por meio de testes, ",
                            "possui suporte a testes automatizados, "
                    ))
            );
        } else {
            context.append(
                    pick(List.of(
                            "não apresenta evidências de testes automatizados, ",
                            "carece de cobertura de testes, ",
                            "não demonstra validação automatizada, "
                    ))
            );
        }

        if (Boolean.TRUE.equals(dto.getUsesGit())) {
            context.append(
                    pick(List.of(
                            "além de utilizar controle de versão com Git. ",
                            "mantendo versionamento estruturado com Git. ",
                            "fazendo uso adequado de controle de versão. "
                    ))
            );
        } else {
            context.append(
                    pick(List.of(
                            "e não evidencia práticas formais de versionamento. ",
                            "sem indicar uso estruturado de versionamento. ",
                            "o que pode impactar rastreabilidade e colaboração. "
                    ))
            );
        }

        return context.toString();
    }

    
    private String buildQualityAnalysis(EvaluationRequestDTO dto, Classification classification) {

        return switch (classification) {

            case EXCELENTE -> pick(List.of(
                    "O conjunto de decisões técnicas indica alta maturidade arquitetural e alinhamento com boas práticas modernas. ",
                    "A estrutura demonstra solidez, coesão e preocupação clara com qualidade e manutenção futura. ",
                    "A implementação revela excelência técnica e forte aderência a princípios de engenharia de software. "
            ));

            case BOM -> pick(List.of(
                    "A solução apresenta consistência estrutural e bom domínio técnico. ",
                    "Observa-se uma base sólida, ainda que existam oportunidades pontuais de refinamento. ",
                    "O projeto demonstra organização e qualidade satisfatória na maior parte dos aspectos avaliados. "
            ));

            case REGULAR -> pick(List.of(
                    "Embora funcional, a implementação poderia evoluir em termos de organização e robustez. ",
                    "Existem pontos estruturais que merecem revisão para elevar o padrão técnico. ",
                    "A base é aceitável, porém há espaço considerável para melhorias arquiteturais. "
            ));

            case RUIM -> pick(List.of(
                    "A estrutura atual evidencia fragilidades que comprometem a qualidade geral da solução. ",
                    "São perceptíveis lacunas importantes em organização, padronização e boas práticas. ",
                    "O projeto necessita de revisões estruturais significativas para atingir um nível técnico adequado. "
            ));
        };
    }

    
    private String buildFinalVerdict(int score, Classification classification) {

        String intensity = switch (classification) {
            case EXCELENTE -> "desempenho excepcional";
            case BOM -> "bom desempenho técnico";
            case REGULAR -> "desempenho mediano";
            case RUIM -> "baixo desempenho técnico";
        };

        return pick(List.of(
                "A pontuação final foi %d/100, refletindo um %s.",
                "Com score %d/100, o projeto demonstra %s.",
                "A avaliação consolidada atingiu %d/100, caracterizando um %s."
        )).formatted(score, intensity);
    }
}