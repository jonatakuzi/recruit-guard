package com.jonatakuzi.recruitguard.rules;

import com.jonatakuzi.recruitguard.domain.CompanyDirectory;
import com.jonatakuzi.recruitguard.domain.DomainAnalyzer;
import com.jonatakuzi.recruitguard.model.Finding;
import com.jonatakuzi.recruitguard.model.Severity;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * If the message claims to be from a company we know, the sender should be
 * using that company's domain. A "Google recruiter" on gmail.com is a red flag.
 */
@Component
public class SenderMismatchRule implements ScanRule {

    private final CompanyDirectory directory;
    private final DomainAnalyzer analyzer;

    public SenderMismatchRule(CompanyDirectory directory, DomainAnalyzer analyzer) {
        this.directory = directory;
        this.analyzer = analyzer;
    }

    @Override
    public List<Finding> evaluate(ScanContext context) {
        String company = context.claimedCompany();
        String domain = context.senderDomain();
        if (company == null || directory.isOfficial(company, domain)) {
            return List.of();
        }
        String official = String.join(", ", directory.domainsFor(company));
        if (analyzer.isFreeProvider(domain)) {
            return List.of(new Finding("free-email-recruiter", Severity.HIGH,
                    "Claims to recruit for " + company + " but writes from a free personal email account. "
                            + company + " recruiters use " + official + ".",
                    domain));
        }
        return List.of(new Finding("sender-not-official", Severity.MEDIUM,
                "Claims to recruit for " + company + " but the sender's domain isn't one of theirs (" + official
                        + "). It could be a third-party agency - verify before replying.",
                domain));
    }
}
