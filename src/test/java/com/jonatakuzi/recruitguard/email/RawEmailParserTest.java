package com.jonatakuzi.recruitguard.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RawEmailParserTest {

    private final RawEmailParser parser = new RawEmailParser();

    private static String example(String name) throws IOException {
        return Files.readString(Path.of("src/main/resources/static/examples", name));
    }

    @Test
    void parsesMultipartScamEmail() throws IOException {
        ParsedEmail email = parser.parse(example("fake-ocient-offer.eml"));

        assertThat(email.from()).isEqualTo("\"Ocient Talent Team\" <hiring@ocient-careers.com>");
        assertThat(email.replyTo()).isEqualTo("ocient.onboarding@gmail.com");
        assertThat(email.subject()).startsWith("Offer Letter");
        // quoted-printable soft line breaks are joined back together
        assertThat(email.body()).contains("selected for the Junior Software Engineer")
                .contains("Social Security number")
                .doesNotContain("<p>");
        assertThat(email.headers()).contains("dmarc=fail");
    }

    @Test
    void convertsHtmlOnlyEmailToText() throws IOException {
        ParsedEmail email = parser.parse(example("real-recruiter.eml"));

        assertThat(email.replyTo()).isNull();
        assertThat(email.body()).contains("I'm a recruiter at Ocient").doesNotContain("<p>", "&#39;");
    }

    @Test
    void decodesEncodedHeaders() {
        String raw = "From: =?UTF-8?B?SsO2cmcgUmVjcnVpdGVy?= <jorg@example.com>\nSubject: hi\n\nHello there";
        ParsedEmail email = parser.parse(raw);

        assertThat(email.from()).isEqualTo("Jörg Recruiter <jorg@example.com>");
        assertThat(email.body()).isEqualTo("Hello there");
    }

    @Test
    void rejectsTextWithoutHeaders() {
        assertThatThrownBy(() -> parser.parse("Just a message body with no headers"))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessageContaining("From:");
    }

    @Test
    void rejectsEmptyInput() {
        assertThatThrownBy(() -> parser.parse("  ")).isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void keepsParagraphsButJoinsWrappedLines() {
        assertThat(RawEmailParser.htmlToText("<p>Hello\n  there</p>\n<p>Second<br>line</p>"))
                .isEqualTo("Hello there\n\nSecond\nline");
    }

    @Test
    void stripsScriptsAndStylesFromHtml() {
        assertThat(RawEmailParser.htmlToText("<style>p{}</style><p>Hi&nbsp;there</p><script>x()</script>"))
                .isEqualTo("Hi there");
    }
}
