package com.jonatakuzi.recruitguard.rules;

import com.jonatakuzi.recruitguard.domain.DomainAnalyzer;
import com.jonatakuzi.recruitguard.model.Finding;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Runs the look-alike domain checks on the From and Reply-To domains. */
@Component
public class ImpersonationRule implements ScanRule {

    private final DomainAnalyzer analyzer;

    public ImpersonationRule(DomainAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public List<Finding> evaluate(ScanContext context) {
        List<Finding> findings = new ArrayList<>(analyzer.analyze(context.senderDomain()));
        if (context.replyToDomain() != null && !context.replyToDomain().equals(context.senderDomain())) {
            findings.addAll(analyzer.analyze(context.replyToDomain()));
        }
        return findings;
    }
}
