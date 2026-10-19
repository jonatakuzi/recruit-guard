package com.jonatakuzi.recruitguard.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The list of real companies and their official email domains, loaded from
 * companies.json. Adding a company means editing JSON, not code.
 */
@Component
public class CompanyDirectory {

    private final Map<String, List<String>> officialDomains;

    public CompanyDirectory() {
        this(loadDefault());
    }

    public CompanyDirectory(Map<String, List<String>> officialDomains) {
        this.officialDomains = Collections.unmodifiableMap(new LinkedHashMap<>(officialDomains));
    }

    public Map<String, List<String>> all() {
        return officialDomains;
    }

    /** Case-insensitive lookup by company name. */
    public Optional<String> findCompany(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        return officialDomains.keySet().stream()
                .filter(company -> company.toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst();
    }

    /** Finds the first known company whose name appears in the text. */
    public Optional<String> detectCompanyIn(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return officialDomains.keySet().stream()
                .filter(company -> lower.matches("(?s).*\\b"
                        + java.util.regex.Pattern.quote(company.toLowerCase(Locale.ROOT)) + "\\b.*"))
                .findFirst();
    }

    public List<String> domainsFor(String company) {
        return officialDomains.getOrDefault(company, List.of());
    }

    /** True if the domain is one of the company's own domains or a subdomain of one. */
    public boolean isOfficial(String company, String domain) {
        String registrable = DomainUtils.registrableDomain(domain);
        return domainsFor(company).contains(registrable);
    }

    private static Map<String, List<String>> loadDefault() {
        try (InputStream in = CompanyDirectory.class.getResourceAsStream("/companies.json")) {
            if (in == null) {
                throw new IllegalStateException("companies.json not found on classpath");
            }
            return new ObjectMapper().readValue(in, new TypeReference<LinkedHashMap<String, List<String>>>() { });
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read companies.json", e);
        }
    }
}
