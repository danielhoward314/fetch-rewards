package com.danielhoward.fetchrewards.svcdata;

import java.time.LocalDateTime;

public class Transaction {

    String payer;

    Long points;

    LocalDateTime timestamp;

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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
