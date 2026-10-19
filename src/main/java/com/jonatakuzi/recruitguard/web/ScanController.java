package com.jonatakuzi.recruitguard.web;

import com.jonatakuzi.recruitguard.domain.CompanyDirectory;
import com.jonatakuzi.recruitguard.domain.DomainAnalyzer;
import com.jonatakuzi.recruitguard.domain.DomainUtils;
import com.jonatakuzi.recruitguard.email.InvalidEmailException;
import com.jonatakuzi.recruitguard.email.ParsedEmail;
import com.jonatakuzi.recruitguard.email.RawEmailParser;
import com.jonatakuzi.recruitguard.model.Finding;
import com.jonatakuzi.recruitguard.model.RawScanResponse;
import com.jonatakuzi.recruitguard.model.ScanReport;
import com.jonatakuzi.recruitguard.model.ScanRequest;
import com.jonatakuzi.recruitguard.service.ScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "Scanner", description = "Score recruiter messages and domains for scam red flags")
public class ScanController {

    static final int MAX_RAW_EMAIL_CHARS = 1_000_000;

    private final ScanService scanService;
    private final DomainAnalyzer domainAnalyzer;
    private final CompanyDirectory directory;
    private final RawEmailParser rawEmailParser;

    public ScanController(ScanService scanService,
                          DomainAnalyzer domainAnalyzer,
                          CompanyDirectory directory,
                          RawEmailParser rawEmailParser) {
        this.scanService = scanService;
        this.domainAnalyzer = domainAnalyzer;
        this.directory = directory;
        this.rawEmailParser = rawEmailParser;
    }

    @PostMapping("/scan")
    @Operation(summary = "Scan a full recruiter message")
    public ScanReport scan(@Valid @RequestBody ScanRequest request) {
        return scanService.scan(request);
    }

    @PostMapping(value = "/scan/raw", consumes = {MediaType.TEXT_PLAIN_VALUE, "message/rfc822"})
    @Operation(summary = "Scan a full raw email (Gmail 'Show original', Outlook 'View source', or a .eml file)")
    public RawScanResponse scanRaw(@RequestBody String rawEmail) {
        if (rawEmail.length() > MAX_RAW_EMAIL_CHARS) {
            throw new InvalidEmailException("That email is too large to scan (limit is 1 MB of text).");
        }
        ParsedEmail email = rawEmailParser.parse(rawEmail);
        String subject = email.subject() == null ? "" : email.subject();
        String text = (subject + "\n\n" + email.body()).strip();
        ScanRequest request = new ScanRequest(
                email.from(), email.replyTo(), null, text.isEmpty() ? "(empty message)" : text, email.headers());
        return new RawScanResponse(email, scanService.scan(request));
    }

    @GetMapping("/domains/check")
    @Operation(summary = "Check a single domain for company impersonation")
    public Map<String, Object> checkDomain(@RequestParam @NotBlank String domain) {
        String cleaned = DomainUtils.domainFromEmail(domain);
        List<Finding> findings = domainAnalyzer.analyze(cleaned);
        boolean official = directory.all().values().stream()
                .anyMatch(domains -> domains.contains(DomainUtils.registrableDomain(cleaned)));
        return Map.of(
                "domain", cleaned,
                "officialCompanyDomain", official,
                "freeEmailProvider", domainAnalyzer.isFreeProvider(cleaned),
                "findings", findings);
    }

    @GetMapping("/companies")
    @Operation(summary = "List the companies and official domains the scanner knows")
    public Set<String> companies() {
        return directory.all().keySet();
    }
}
