package com.jonatakuzi.recruitguard.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.jonatakuzi.recruitguard.model.Finding;
import com.jonatakuzi.recruitguard.model.ScanRequest;
import com.jonatakuzi.recruitguard.model.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmailAuthenticationRuleTest {

    private final EmailAuthenticationRule rule = new EmailAuthenticationRule();

    private List<Finding> run(String headers) {
        ScanRequest request = new ScanRequest("a@b.com", null, null, "hello", headers);
        return rule.evaluate(ScanContext.of(request, null));
    }

    @Test
    void dmarcFailureIsHighSeverity() {
        List<Finding> findings = run("Authentication-Results: mx; spf=pass; dkim=pass; dmarc=fail");
        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.ruleId()).isEqualTo("email-auth-dmarc");
            assertThat(f.severity()).isEqualTo(Severity.HIGH);
        });
    }

    @Test
    void softFailIsLowSeverity() {
        assertThat(run("Authentication-Results: mx; spf=softfail"))
                .extracting(Finding::severity).containsExactly(Severity.LOW);
    }

    @Test
    void allPassingProducesNothing() {
        assertThat(run("Authentication-Results: mx; spf=pass; dkim=pass; dmarc=pass")).isEmpty();
    }

    @Test
    void ignoresMissingHeaders() {
        assertThat(run(null)).isEmpty();
        assertThat(run("Subject: hi")).isEmpty();
    }
}
