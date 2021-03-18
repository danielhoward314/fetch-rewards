package com.danielhoward.fetchrewards.svcdata;

public class PointsUsageSummary {

    String payer;

    Long points;

    public PointsUsageSummary() {}

    public PointsUsageSummary(String payer, Long points) {
        this.payer = payer;
        this.points = points;
    }

    public String getPayer() {
        return payer;
    }

    public void setPayer(String payer) {
        this.payer = payer;
    }

    public Long getPoints() {
        return points;
    }

    public void setPoints(Long points) {
        this.points = points;
    }

}
