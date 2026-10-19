package com.jonatakuzi.recruitguard.model;

import java.util.List;

/**
 * The result returned to the user.
 *
 * @param score          0-100 risk score (sum of finding points, capped at 100)
 * @param verdict        human-readable summary of the score
 * @param claimedCompany the company the message claims to be from, or null
 * @param findings       every red flag found, most severe first
 */
public record ScanReport(int score, Verdict verdict, String claimedCompany, List<Finding> findings) {
}
