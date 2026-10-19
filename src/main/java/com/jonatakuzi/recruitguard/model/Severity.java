package com.jonatakuzi.recruitguard.model;

/**
 * How serious a red flag is. Each level adds a fixed number of risk points,
 * so the final score is easy to explain: it is just the sum of the findings.
 */
public enum Severity {
    INFO(0),
    LOW(5),
    MEDIUM(15),
    HIGH(30);

    private final int points;

    Severity(int points) {
        this.points = points;
    }

    public int points() {
        return points;
    }
}
