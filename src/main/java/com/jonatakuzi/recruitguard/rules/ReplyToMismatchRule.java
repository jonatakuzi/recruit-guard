package com.jonatakuzi.recruitguard.rules;

import com.jonatakuzi.recruitguard.domain.DomainUtils;
import com.jonatakuzi.recruitguard.model.Finding;
import com.jonatakuzi.recruitguard.model.Severity;
import java.util.List;
import org.springframework.stereotype.Component;

/** Flags messages whose replies would go to a different organization than the sender. */
@Component
public class ReplyToMismatchRule implements ScanRule {

    @Override
    public List<Finding> evaluate(ScanContext context) {
        if (context.replyToDomain() == null) {
            return List.of();
        }
        String sender = DomainUtils.registrableDomain(context.senderDomain());
        String replyTo = DomainUtils.registrableDomain(context.replyToDomain());
        if (sender.equals(replyTo)) {
            return List.of();
        }
        return List.of(new Finding("reply-to-mismatch", Severity.MEDIUM,
                "Your reply would go to " + replyTo + ", not the sender's domain " + sender
                        + ". Scammers do this to send from one address and collect replies at another.",
                replyTo));
    }
}
