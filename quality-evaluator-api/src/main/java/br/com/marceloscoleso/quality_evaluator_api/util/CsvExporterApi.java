package br.com.marceloscoleso.quality_evaluator_api.util;

import br.com.marceloscoleso.quality_evaluator_api.model.Evaluation;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CsvExporterApi {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static byte[] export(List<Evaluation> evaluations) {

        StringBuilder csv = new StringBuilder();
        csv.append("Projeto,Linguagem,Nota,Classificacao,Data\n");

        for (Evaluation e : evaluations) {
            csv.append(String.format(
                    "%s,%s,%d,%s,%s%n",
                    e.getProjectName(),
                    e.getLanguage().getDisplayName(),
                    e.getScore(),
                    e.getClassification(),
                    e.getCreatedAt().format(FORMATTER)
            ));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
