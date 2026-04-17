package com.cognivita.dashboardservice.dto;

public class MmseMonthlySeverityDto {

    private final String month;
    private final Long totalTests;
    private final Long normalCount;
    private final Long mildCount;
    private final Long moderateCount;
    private final Long severeCount;
    private final Double avgScore;

    public MmseMonthlySeverityDto(Object month,
                                  Number totalTests,
                                  Number normalCount,
                                  Number mildCount,
                                  Number moderateCount,
                                  Number severeCount,
                                  Number avgScore) {
        this.month = month == null ? null : month.toString();
        this.totalTests = toLong(totalTests);
        this.normalCount = toLong(normalCount);
        this.mildCount = toLong(mildCount);
        this.moderateCount = toLong(moderateCount);
        this.severeCount = toLong(severeCount);
        this.avgScore = avgScore == null ? null : avgScore.doubleValue();
    }

    public String getMonth() {
        return month;
    }

    public Long getTotalTests() {
        return totalTests;
    }

    public Long getNormalCount() {
        return normalCount;
    }

    public Long getMildCount() {
        return mildCount;
    }

    public Long getModerateCount() {
        return moderateCount;
    }

    public Long getSevereCount() {
        return severeCount;
    }

    public Double getAvgScore() {
        return avgScore;
    }

    private Long toLong(Number value) {
        return value == null ? 0L : value.longValue();
    }
}
