package com.danielhoward.fetchrewards.svcdata;

import java.util.List;

public class UpdateLedger {

    List<Integer> indicesToRemove;

    Long pointsSpent;

    public UpdateLedger() {}

    public UpdateLedger(List<Integer> indicesToRemove, Long pointsSpent) {
        this.indicesToRemove = indicesToRemove;
        this.pointsSpent = pointsSpent;
    }

    public List<Integer> getIndicesToRemove() {
        return indicesToRemove;
    }

    public void setIndicesToRemove(List<Integer> indicesToRemove) {
        this.indicesToRemove = indicesToRemove;
    }

    public Long getPointsSpent() {
        return pointsSpent;
    }

    public void setPointsSpent(Long pointsSpent) {
        this.pointsSpent = pointsSpent;
    }

}
