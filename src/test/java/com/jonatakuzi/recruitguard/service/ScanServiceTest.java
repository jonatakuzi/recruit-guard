package com.jonatakuzi.recruitguard.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jonatakuzi.recruitguard.model.Finding;
import com.jonatakuzi.recruitguard.model.ScanReport;
import com.jonatakuzi.recruitguard.model.ScanRequest;
import com.jonatakuzi.recruitguard.model.Verdict;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Runs the real, fully wired scanner end-to-end on realistic messages. */
@SpringBootTest
class ScanServiceTest {

    @Autowired
    private ScanService scanService;

    @Test
    void flagsClassicImpersonationScam() {
        ScanRequest scam = new ScanRequest(
                "Talent Team <hiring@ocient-careers.com>",
                "ocient.onboarding@gmail.com",
                null,
                """
                Congratulations! You have been selected for the Junior Software Engineer role at Ocient.
                No interview is required. Please reply within 24 hours with your Social Security number
                and bank account details for direct deposit. We will mail you a check to purchase
                your equipment from our vendor.
                """,
                "Authentication-Results: mx.google.com; spf=fail; dkim=none; dmarc=fail");

        ScanReport report = scanService.scan(scam);

        assertThat(report.verdict()).isEqualTo(Verdict.LIKELY_SCAM);
        assertThat(report.score()).isEqualTo(100);
        assertThat(report.claimedCompany()).isEqualTo("Ocient");
        assertThat(report.findings()).extracting(Finding::ruleId).contains(
                "brand-embedded",
                "sender-not-official",
                "reply-to-mismatch",
                "sensitive-info-request",
                "payment-scheme",
                "too-good-to-be-true",
                "pressure-tactics",
                "email-auth-spf",
                "email-auth-dmarc");
    }

    @Test
    void findingsAreSortedMostSevereFirst() {
        ScanReport report = scanService.scan(new ScanRequest(
                "x@gmail.com", null, "Tesla",
                "Respond urgently. Send your SSN.", null));

        assertThat(report.findings()).isSortedAccordingTo(
                (a, b) -> Integer.compare(b.points(), a.points()));
    }

    @Test
    void passesLegitimateRecruiterMessage() {
        ScanRequest legit = new ScanRequest(
                "Sarah Lee <slee@ocient.com>",
                null,
                null,
                """
                Hi Jon, I'm a recruiter at Ocient. I came across your profile and think you'd be a
                good fit for our Associate Software Engineer opening. Are you free for a 30-minute
                phone screen next week? You can also see the posting on our careers page.
                """,
                "Authentication-Results: mx.google.com; spf=pass; dkim=pass; dmarc=pass");

        ScanReport report = scanService.scan(legit);

        assertThat(report.verdict()).isEqualTo(Verdict.LOW_RISK);
        assertThat(report.findings()).isEmpty();
    }

    @Test
    void flagsFreeEmailRecruiterForNamedCompany() {
        ScanReport report = scanService.scan(new ScanRequest(
                "google.recruiting.team@gmail.com", null, "google",
                "We'd like to set up a chat interview on Telegram.", null));

        assertThat(report.findings()).extracting(Finding::ruleId)
                .contains("free-email-recruiter", "off-platform-interview");
        assertThat(report.verdict()).isEqualTo(Verdict.SUSPICIOUS);
    }
}
