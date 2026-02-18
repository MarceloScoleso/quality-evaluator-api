package br.com.marceloscoleso.quality_evaluator_api.dto;

public class EvaluationStatsDTO {

    private long total;
    private double averageScore;
    private long excellentCount;

    public EvaluationStatsDTO(long total, double averageScore, long excellentCount) {
        this.total = total;
        this.averageScore = averageScore;
        this.excellentCount = excellentCount;
    }

    public long getTotal() {
        return total;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public long getExcellentCount() {
        return excellentCount;
    }
}
