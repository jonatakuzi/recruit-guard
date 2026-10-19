package com.jonatakuzi.recruitguard.rules;

import com.jonatakuzi.recruitguard.domain.DomainUtils;
import com.jonatakuzi.recruitguard.model.ScanRequest;
import java.util.Locale;

/**
 * The request, pre-processed once so every rule doesn't repeat the same parsing.
 *
 * @param claimedCompany the known company this message claims to be from, or null
 */
public record ScanContext(
        ScanRequest request,
        String senderDomain,
        String replyToDomain,
        String claimedCompany,
        String bodyLower,
        String headers) {

    public static ScanContext of(ScanRequest request, String claimedCompany) {
        String replyTo = request.replyTo() == null || request.replyTo().isBlank()
                ? null
                : DomainUtils.domainFromEmail(request.replyTo());
        return new ScanContext(
                request,
                DomainUtils.domainFromEmail(request.sender()),
                replyTo,
                claimedCompany,
                request.body().toLowerCase(Locale.ROOT),
                request.headers() == null ? "" : request.headers());
    }
}
