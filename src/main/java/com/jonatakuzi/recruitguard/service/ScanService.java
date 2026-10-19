package com.jonatakuzi.recruitguard.service;

import com.jonatakuzi.recruitguard.domain.CompanyDirectory;
import com.jonatakuzi.recruitguard.model.Finding;
import com.jonatakuzi.recruitguard.model.ScanReport;
import com.jonatakuzi.recruitguard.model.ScanRequest;
import com.jonatakuzi.recruitguard.model.Verdict;
import com.jonatakuzi.recruitguard.rules.ScanContext;
import com.jonatakuzi.recruitguard.rules.ScanRule;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/** Runs every rule against a message and turns the findings into a score. */
@Service
public class ScanService {

    static final int MAX_SCORE = 100;

    private final List<ScanRule> rules;
    private final CompanyDirectory directory;

    public ScanService(List<ScanRule> rules, CompanyDirectory directory) {
        this.rules = rules;
        this.directory = directory;
    }

    public ScanReport scan(ScanRequest request) {
        String company = resolveCompany(request);
        ScanContext context = ScanContext.of(request, company);

        List<Finding> findings = rules.stream()
                .flatMap(rule -> rule.evaluate(context).stream())
                .sorted(Comparator.comparingInt(Finding::points).reversed())
                .toList();

        int score = Math.min(MAX_SCORE, findings.stream().mapToInt(Finding::points).sum());
        return new ScanReport(score, Verdict.fromScore(score), company, findings);
    }

    /** Uses the company the user typed, or else looks for one named in the message. */
    private String resolveCompany(ScanRequest request) {
        return directory.findCompany(request.claimedCompany())
                .or(() -> directory.detectCompanyIn(request.body()))
                .orElse(null);
    }
}
