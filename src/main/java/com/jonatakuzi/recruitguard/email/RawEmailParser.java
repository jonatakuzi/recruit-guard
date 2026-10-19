package com.jonatakuzi.recruitguard.email;

import jakarta.mail.BodyPart;
import jakarta.mail.Header;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Properties;
import org.springframework.stereotype.Component;

/**
 * Turns a raw email (what Gmail calls "Show original", or a saved .eml file)
 * into a ParsedEmail. Jakarta Mail handles the hard parts of the MIME format:
 * folded headers, multipart bodies, base64 and quoted-printable encodings.
 */
@Component
public class RawEmailParser {

    /** Stop digging after this many nested multipart levels (protects against malicious input). */
    private static final int MAX_DEPTH = 10;

    private final Session session = Session.getInstance(new Properties());

    public ParsedEmail parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidEmailException("The email is empty.");
        }
        try {
            MimeMessage message = new MimeMessage(session,
                    new ByteArrayInputStream(normalizeLineEndings(raw).getBytes(StandardCharsets.UTF_8)));

            String from = header(message, "From");
            if (from == null) {
                throw new InvalidEmailException(
                        "Couldn't find a From: line. Paste the full original email, including the headers at the top.");
            }
            String body = extractText(message, 0);
            return new ParsedEmail(from, header(message, "Reply-To"), message.getSubject(),
                    body == null ? "" : body.strip(), allHeaders(message));
        } catch (MessagingException | IOException e) {
            throw new InvalidEmailException("Couldn't read that email: " + e.getMessage());
        }
    }

    /** Finds readable text: prefers text/plain, falls back to text/html converted to plain text. */
    private String extractText(Part part, int depth) throws MessagingException, IOException {
        if (depth > MAX_DEPTH) {
            return null;
        }
        if (part.isMimeType("text/plain")) {
            return part.getContent().toString();
        }
        if (part.isMimeType("text/html")) {
            return htmlToText(part.getContent().toString());
        }
        if (part.isMimeType("multipart/*") && part.getContent() instanceof Multipart multipart) {
            String htmlFallback = null;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart child = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(child.getDisposition())) {
                    continue;
                }
                if (child.isMimeType("text/plain")) {
                    return child.getContent().toString();
                }
                String text = extractText(child, depth + 1);
                if (text != null && htmlFallback == null) {
                    htmlFallback = text;
                }
            }
            return htmlFallback;
        }
        return null;
    }

    static String htmlToText(String html) {
        return html
                .replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("\\s+", " ")                                   // source line breaks mean nothing in HTML
                .replaceAll("(?i)<br\\s*/?>", "\n")                        // line break
                .replaceAll("(?i)</(p|div|li|tr|h[1-6])>", "\n\n")         // end of a block = new paragraph
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private static String header(MimeMessage message, String name) throws MessagingException {
        String value = message.getHeader(name, ", ");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MimeUtility.decodeText(MimeUtility.unfold(value)).strip();
        } catch (UnsupportedEncodingException e) {
            return value.strip();
        }
    }

    private static String allHeaders(MimeMessage message) throws MessagingException {
        StringBuilder sb = new StringBuilder();
        Enumeration<Header> headers = message.getAllHeaders();
        while (headers.hasMoreElements()) {
            Header h = headers.nextElement();
            sb.append(h.getName()).append(": ").append(MimeUtility.unfold(h.getValue())).append('\n');
        }
        return sb.toString();
    }

    private static String normalizeLineEndings(String raw) {
        return raw.replace("\r\n", "\n").replace("\n", "\r\n");
    }
}
