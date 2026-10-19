package com.jonatakuzi.recruitguard.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.jonatakuzi.recruitguard.model.Finding;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DomainAnalyzerTest {

    private final DomainAnalyzer analyzer = new DomainAnalyzer(new CompanyDirectory());

    @ParameterizedTest
    @CsvSource({
            "ocient.com.hr-portal.net, brand-in-subdomain",
            "ocient.co, suffix-swap",
            "0cient.com, lookalike-characters",
            "ocient-careers.com, brand-embedded",
            "ocienthr.com, brand-embedded",
            "ocint.com, typosquat",
            "rnicrosoft.com, lookalike-characters"
    })
    void catchesImpersonationTricks(String domain, String expectedRule) {
        List<Finding> findings = analyzer.analyze(domain);
        assertThat(findings).extracting(Finding::ruleId).contains(expectedRule);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ocient.com", "careers.ocient.com", "amazon.jobs", "ngc.com", "unrelated-shop.com"})
    void leavesRealAndUnrelatedDomainsAlone(String domain) {
        assertThat(analyzer.analyze(domain)).isEmpty();
    }

    @Test
    void handlesBlankInput() {
        assertThat(analyzer.analyze("")).isEmpty();
        assertThat(analyzer.analyze(null)).isEmpty();
    }

    @Test
    void recognizesFreeEmailProviders() {
        assertThat(analyzer.isFreeProvider("gmail.com")).isTrue();
        assertThat(analyzer.isFreeProvider("ocient.com")).isFalse();
    }
}
