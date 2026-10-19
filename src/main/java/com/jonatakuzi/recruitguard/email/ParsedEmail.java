package com.jonatakuzi.recruitguard.email;

/**
 * The useful parts of a raw email after parsing.
 *
 * @param from    decoded From header, e.g. "Talent Team <hiring@ocient-careers.com>"
 * @param replyTo decoded Reply-To header, or null
 * @param subject decoded Subject, or null
 * @param body    plain-text body (HTML is converted to text)
 * @param headers every header, one per line, for the authentication checks
 */
public record ParsedEmail(String from, String replyTo, String subject, String body, String headers) {
}
