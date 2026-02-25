package br.com.marceloscoleso.quality_evaluator_api.dto;

import java.time.LocalDate;
import java.util.Map;

public class DashboardSummaryDTO {

    private long total;
    private long excellent;
    private long good;
    private long regular;
    private long bad;

    private double averageScore;

    private Map<String, Long> byLanguage;
    private Map<LocalDate, Double> scoreEvolution;

    private double testsPercentage;
    private double gitPercentage;

    public DashboardSummaryDTO(
            long total,
            long excellent,
            long good,
            long regular,
            long bad,
            double averageScore,
            Map<String, Long> byLanguage,
            Map<LocalDate, Double> scoreEvolution,
            double testsPercentage,
            double gitPercentage
    ) {
        this.total = total;
        this.excellent = excellent;
        this.good = good;
        this.regular = regular;
        this.bad = bad;
        this.averageScore = averageScore;
        this.byLanguage = byLanguage;
        this.scoreEvolution = scoreEvolution;
        this.testsPercentage = testsPercentage;
        this.gitPercentage = gitPercentage;
    }

    public long getTotal() { return total; }
    public long getExcellent() { return excellent; }
    public long getGood() { return good; }
    public long getRegular() { return regular; }
    public long getBad() { return bad; }
    public double getAverageScore() { return averageScore; }
    public Map<String, Long> getByLanguage() { return byLanguage; }
    public Map<LocalDate, Double> getScoreEvolution() { return scoreEvolution; }
    public double getTestsPercentage() { return testsPercentage; }
    public double getGitPercentage() { return gitPercentage; }
}