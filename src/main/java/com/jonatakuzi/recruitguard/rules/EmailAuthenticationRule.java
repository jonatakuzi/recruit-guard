package com.jonatakuzi.recruitguard.rules;

import com.jonatakuzi.recruitguard.model.Finding;
import com.jonatakuzi.recruitguard.model.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Reads the Authentication-Results header that mail servers add.
 * SPF   - was the sending server allowed to send for this domain?
 * DKIM  - was the message cryptographically signed by the domain?
 * DMARC - does the domain's own policy say this message passes?
 */
@Component
public class EmailAuthenticationRule implements ScanRule {

    private static final Pattern RESULT = Pattern.compile("\\b(spf|dkim|dmarc)=(\\w+)", Pattern.CASE_INSENSITIVE);

    @Override
    public List<Finding> evaluate(ScanContext context) {
        String headers = context.headers();
        if (headers.isBlank() || !headers.toLowerCase(Locale.ROOT).contains("authentication-results")) {
            return List.of();
        }
        List<Finding> findings = new ArrayList<>();
        Matcher matcher = RESULT.matcher(headers);
        while (matcher.find()) {
            String check = matcher.group(1).toUpperCase(Locale.ROOT);
            String result = matcher.group(2).toLowerCase(Locale.ROOT);
            Severity severity = switch (result) {
                case "fail" -> check.equals("DMARC") ? Severity.HIGH : Severity.MEDIUM;
                case "softfail", "none", "neutral", "permerror", "temperror" -> Severity.LOW;
                default -> null; // "pass" or unknown - nothing to report
            };
            if (severity != null) {
                findings.add(new Finding("email-auth-" + check.toLowerCase(Locale.ROOT), severity,
                        check + " check returned '" + result + "'. The message may not really come from the domain it claims.",
                        matcher.group()));
            }
        }
        return findings;
    }
}
