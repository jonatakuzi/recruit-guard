package com.jonatakuzi.recruitguard.model;

public enum Verdict {
    LOW_RISK("No major red flags found. Still verify the role on the company's own careers site."),
    SUSPICIOUS("Suspicious. Don't share personal info until you verify this recruiter independently."),
    LIKELY_SCAM("Likely a scam. Do not reply with personal or financial information.");

    private final String advice;

    Verdict(String advice) {
        this.advice = advice;
    }

    public String getAdvice() {
        return advice;
    }

    /** Converts a 0-100 score into a verdict. Thresholds are deliberately simple. */
    public static Verdict fromScore(int score) {
        if (score >= 50) {
            return LIKELY_SCAM;
        }
        if (score >= 20) {
            return SUSPICIOUS;
        }
        return LOW_RISK;
    }
}
