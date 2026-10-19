package com.jonatakuzi.recruitguard.model;

/**
 * One red flag found in a message.
 *
 * @param ruleId   short machine-readable name, e.g. "lookalike-domain"
 * @param severity how serious it is
 * @param message  plain-English explanation for the user
 * @param evidence the exact text or domain that triggered the rule
 */
public record Finding(String ruleId, Severity severity, String message, String evidence) {

    public int points() {
        return severity.points();
    }
}
