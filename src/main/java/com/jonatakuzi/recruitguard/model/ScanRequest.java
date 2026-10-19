package com.jonatakuzi.recruitguard.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What the user submits: the recruiter's email address and message, plus
 * optional extras that make the scan more accurate.
 *
 * @param sender      the From address, e.g. "jane@ocient-careers.com"
 * @param replyTo     the Reply-To address, if different (optional)
 * @param claimedCompany the company the recruiter says they represent (optional;
 *                    detected from the message body if left blank)
 * @param body        the message text
 * @param headers     raw email headers, if the user copied them (optional)
 */
public record ScanRequest(
        @NotBlank @Size(max = 320) String sender,
        @Size(max = 320) String replyTo,
        @Size(max = 100) String claimedCompany,
        @NotBlank @Size(max = 50_000) String body,
        @Size(max = 50_000) String headers) {
}
