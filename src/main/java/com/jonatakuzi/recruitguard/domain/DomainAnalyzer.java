package com.jonatakuzi.recruitguard.domain;

import com.jonatakuzi.recruitguard.model.Finding;
import com.jonatakuzi.recruitguard.model.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Checks a sender's domain against every known company to catch impersonation:
 * look-alike characters, typos, brand names stuffed into a new domain, swapped
 * suffixes, and brand names hidden in subdomains.
 */
@Component
public class DomainAnalyzer {

    static final Set<String> FREE_EMAIL_PROVIDERS = Set.of(
            "gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "aol.com",
            "icloud.com", "proton.me", "protonmail.com", "gmx.com", "mail.com", "yandex.com");

    /** Don't run typo checks on very short names - "ngc" vs "nbc" is too noisy. */
    private static final int MIN_NAME_LENGTH_FOR_TYPO_CHECK = 5;

    private final CompanyDirectory directory;

    public DomainAnalyzer(CompanyDirectory directory) {
        this.directory = directory;
    }

    public boolean isFreeProvider(String domain) {
        return FREE_EMAIL_PROVIDERS.contains(DomainUtils.registrableDomain(domain));
    }

    /** Runs every impersonation check against every company in the directory. */
    public List<Finding> analyze(String domain) {
        List<Finding> findings = new ArrayList<>();
        if (domain == null || domain.isBlank()) {
            return findings;
        }
        String fullDomain = domain.toLowerCase();
        String registrable = DomainUtils.registrableDomain(fullDomain);

        for (Map.Entry<String, List<String>> entry : directory.all().entrySet()) {
            String company = entry.getKey();
            if (entry.getValue().contains(registrable)) {
                return List.of(); // It's a real company domain - nothing to flag.
            }
            for (String official : entry.getValue()) {
                Finding finding = compare(company, official, fullDomain, registrable);
                if (finding != null) {
                    findings.add(finding);
                    break; // One finding per company is enough.
                }
            }
        }
        return findings;
    }

    /** Returns the first impersonation pattern that matches, or null. */
    private Finding compare(String company, String official, String fullDomain, String registrable) {
        String officialName = DomainUtils.nameWithoutSuffix(official);
        String senderName = DomainUtils.nameWithoutSuffix(registrable);

        // 1. Brand hidden in a subdomain: ocient.com.hr-portal.net
        if (fullDomain.contains(official + ".")) {
            return high("brand-in-subdomain", company, fullDomain,
                    "'" + official + "' appears in the address, but the domain is really owned by " + registrable + ".");
        }
        // 2. Same name, different suffix: ocient.co, ocient.net
        if (senderName.equals(officialName)) {
            return high("suffix-swap", company, registrable,
                    "Uses ." + DomainUtils.suffix(registrable) + " instead of the official " + official + ".");
        }
        // 3. Look-alike characters: 0cient.com, rnicrosoft.com
        if (DomainUtils.normalizeLookalikes(senderName).equals(officialName)) {
            return high("lookalike-characters", company, registrable,
                    "Swaps in look-alike characters to imitate " + official + ".");
        }
        // 4. Brand plus extra words: ocient-careers.com, hrocient.com
        if (officialName.length() >= 4 && DomainUtils.normalizeLookalikes(senderName).contains(officialName)) {
            return high("brand-embedded", company, registrable,
                    "Contains '" + officialName + "' but is not an official " + company + " domain.");
        }
        // 5. Small typo: ocint.com, occient.com
        if (officialName.length() >= MIN_NAME_LENGTH_FOR_TYPO_CHECK) {
            int distance = DomainUtils.editDistance(senderName, officialName);
            if (distance > 0 && distance <= 2) {
                return high("typosquat", company, registrable,
                        "Is only " + distance + " character(s) different from " + official + ".");
            }
        }
        return null;
    }

    private static Finding high(String ruleId, String company, String evidence, String detail) {
        return new Finding(ruleId, Severity.HIGH, "Possible " + company + " impersonation. " + detail, evidence);
    }
}
