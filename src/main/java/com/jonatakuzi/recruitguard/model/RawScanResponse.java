package com.jonatakuzi.recruitguard.model;

import com.jonatakuzi.recruitguard.email.ParsedEmail;

/** Result of scanning a raw email: what we read out of it, plus the report. */
public record RawScanResponse(ParsedEmail email, ScanReport report) {
}
