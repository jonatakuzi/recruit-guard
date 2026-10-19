package com.jonatakuzi.recruitguard.rules;

import com.jonatakuzi.recruitguard.model.Finding;
import com.jonatakuzi.recruitguard.model.Severity;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for rules that look for risky phrases in the message body.
 * Subclasses only supply their patterns; this class does the matching
 * (the Template Method pattern).
 */
public abstract class KeywordRule implements ScanRule {

    private final String ruleId;
    private final Severity severity;
    private final String message;
    private final List<Pattern> patterns;

    protected KeywordRule(String ruleId, Severity severity, String message, List<String> regexes) {
        this.ruleId = ruleId;
        this.severity = severity;
        this.message = message;
        this.patterns = regexes.stream()
                .map(regex -> Pattern.compile(regex, Pattern.CASE_INSENSITIVE))
                .toList();
    }

    @Override
    public List<Finding> evaluate(ScanContext context) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(context.bodyLower());
            if (matcher.find()) {
                return List.of(new Finding(ruleId, severity, message, matcher.group()));
            }
        }
        return List.of();
    }
}
