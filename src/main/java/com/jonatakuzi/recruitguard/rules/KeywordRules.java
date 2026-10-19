package com.jonatakuzi.recruitguard.rules;

import com.jonatakuzi.recruitguard.model.Severity;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The phrase-based checks. Each is tiny because KeywordRule does the work.
 * Patterns are based on the FTC's published job-scam warning signs.
 */
public final class KeywordRules {

    private KeywordRules() {
    }

    @Component
    public static class SensitiveInfoRule extends KeywordRule {
        public SensitiveInfoRule() {
            super("sensitive-info-request", Severity.HIGH,
                    "Asks for identity or banking details. Real employers collect these only after a formal offer, through HR systems.",
                    List.of(
                            "social security( number)?",
                            "\\bssn\\b",
                            "bank account( number| details| info)?",
                            "routing number",
                            "driver'?s licen[cs]e( number)?",
                            "passport (number|copy|scan)",
                            "date of birth"));
        }
    }

    @Component
    public static class PaymentRule extends KeywordRule {
        public PaymentRule() {
            super("payment-scheme", Severity.HIGH,
                    "Mentions sending you a check or having you buy equipment - the classic fake-check job scam.",
                    List.of(
                            "(deposit|cash) (the|a|this) check",
                            "check (to|for) (purchase|buy)",
                            "(purchase|buy) (your |the )?(equipment|laptop|software) (from|through) (our|a|the) vendor",
                            "gift cards?",
                            "\\b(zelle|cash ?app|venmo|bitcoin|crypto(currency)?)\\b",
                            "training fee",
                            "pay (a|the) (fee|deposit)"));
        }
    }

    @Component
    public static class OffPlatformChatRule extends KeywordRule {
        public OffPlatformChatRule() {
            super("off-platform-interview", Severity.MEDIUM,
                    "Wants to interview over a chat app. Legitimate companies interview by phone or video call.",
                    List.of(
                            "\\b(telegram|whatsapp|signal app|wechat|google hangouts?)\\b",
                            "(text|chat)[- ]based interview",
                            "interview (will be )?(conducted )?(via|over|on) (text|chat)"));
        }
    }

    @Component
    public static class TooGoodToBeTrueRule extends KeywordRule {
        public TooGoodToBeTrueRule() {
            super("too-good-to-be-true", Severity.MEDIUM,
                    "Offers a job with little or no real interview process.",
                    List.of(
                            "no interview (is )?(required|needed)",
                            "(you('ve| have) been|you are) (selected|hired|approved) (for|without)",
                            "hired (immediately|on the spot)",
                            "guaranteed (job|position|income)"));
        }
    }

    @Component
    public static class UrgencyRule extends KeywordRule {
        public UrgencyRule() {
            super("pressure-tactics", Severity.LOW,
                    "Uses urgency to rush you. Scammers want you acting before you think.",
                    List.of(
                            "within (the next )?(24|48) hours",
                            "\\b(urgent(ly)?|immediately|asap|act now)\\b",
                            "(offer|position) (will )?expires?"));
        }
    }
}
