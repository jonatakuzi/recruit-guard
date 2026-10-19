package com.jonatakuzi.recruitguard.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DomainUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "jane@ocient.com, ocient.com",
            "'Jane Doe <Jane@Ocient-Careers.com>', ocient-careers.com",
            "ocient.com, ocient.com",
            "'  bob@example.org. ', example.org"
    })
    void extractsDomainFromEmail(String input, String expected) {
        assertThat(DomainUtils.domainFromEmail(input)).isEqualTo(expected);
    }

    @Test
    void domainFromNullIsEmpty() {
        assertThat(DomainUtils.domainFromEmail(null)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "careers.ocient.com, ocient.com",
            "ocient.com.hr-portal.net, hr-portal.net",
            "jobs.example.co.uk, example.co.uk",
            "ocient.com, ocient.com"
    })
    void findsRegistrableDomain(String input, String expected) {
        assertThat(DomainUtils.registrableDomain(input)).isEqualTo(expected);
    }

    @Test
    void splitsNameAndSuffix() {
        assertThat(DomainUtils.nameWithoutSuffix("example.co.uk")).isEqualTo("example");
        assertThat(DomainUtils.suffix("example.co.uk")).isEqualTo("co.uk");
        assertThat(DomainUtils.suffix("localhost")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "0cient, ocient",
            "rnicrosoft, microsoft",
            "g00gle, google",
            "te5la, tesla",
            "ama-zon, amazon"
    })
    void undoesLookalikeCharacters(String input, String expected) {
        assertThat(DomainUtils.normalizeLookalikes(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "ocient, ocient, 0",
            "ocint, ocient, 1",
            "occient, ocient, 1",
            "kitten, sitting, 3",
            "'', abc, 3"
    })
    void computesEditDistance(String a, String b, int expected) {
        assertThat(DomainUtils.editDistance(a, b)).isEqualTo(expected);
    }
}
