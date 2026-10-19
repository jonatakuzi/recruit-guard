# Recruit Guard

![CI](https://github.com/jonatakuzi/recruit-guard/actions/workflows/ci.yml/badge.svg)
![Release](https://img.shields.io/github/v/release/jonatakuzi/recruit-guard)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
![Coverage](https://img.shields.io/badge/coverage-97%25-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)

**Check a recruiter's email for job-scam red flags before you reply.**

Paste in the full email (or drop a `.eml` file) and Recruit Guard reads the sender, the hidden Reply-To address, and the mail server's SPF/DKIM/DMARC results, then marks every warning sign right inside the message, the way a spell checker marks typos.

![A fake job offer with each warning sign underlined and explained](docs/images/dashboard.png)

## Why I built this

During my own job search, I was contacted by a "recruiter" for Ocient who was actually writing from a spoofed look-alike domain. I caught it because I checked the domain by hand, but most people wouldn't think to. Job seekers, especially new grads applying to dozens of roles, are exactly who these scams target, and the warning signs are usually detectable by a program.

So I turned the checks I was doing by hand into software: *Is this really the company's domain? Why does the reply go to Gmail? Why do they want my SSN before an offer?*

## Try it in one minute

**With Docker** (nothing else to install):

```bash
docker run -p 8080:8080 ghcr.io/jonatakuzi/recruit-guard
```

**Or download the app** (needs Java 21): grab `recruit-guard-*.jar` from the [latest release](https://github.com/jonatakuzi/recruit-guard/releases/latest), then:

```bash
java -jar recruit-guard-1.1.1.jar
```

Open **http://localhost:8080**, click **Try a fake offer**, and you'll see the result above.

### Checking a real email

1. Open the email in your inbox and copy the full original:
   - **Gmail:** three dots next to Reply → *Show original* → *Copy to clipboard*
   - **Outlook:** three dots → *View* → *View message source*
   - **Apple Mail:** *View* → *Message* → *Raw Source*
2. Paste it into Recruit Guard and click **Check this email**.

You can also drag a saved `.eml` file onto the page. Everything runs on your own computer, so the email never leaves it.

## What it checks

| Check | What it catches | Example |
|---|---|---|
| Look-alike characters | Digits swapped for letters | `0cient.com`, `rnicrosoft.com` |
| Typosquatting | Domains 1-2 edits away (Levenshtein distance) | `ocint.com` |
| Brand embedding | Real name plus extra words | `ocient-careers.com` |
| Suffix swap | Right name, wrong ending | `ocient.co` |
| Subdomain trick | Brand hidden in front of another domain | `ocient.com.hr-portal.net` |
| Sender mismatch | "Google recruiter" on a personal Gmail | `google.team@gmail.com` |
| Reply-To mismatch | Replies secretly routed elsewhere | From `acme.com`, Reply-To `gmail.com` |
| Email authentication | Failed SPF / DKIM / DMARC | `dmarc=fail` |
| Sensitive data requests | SSN, bank, routing, license before an offer | "send your SSN" |
| Payment schemes | Fake checks, gift cards, crypto, "training fees" | "deposit the check" |
| Off-platform interviews | Telegram / WhatsApp "interviews" | "interview over Telegram" |
| Pressure tactics | Manufactured urgency | "within 24 hours" |

Results come with plain-English next steps and a **Copy results** button, so you can send the findings to a friend, a career center, or the company's real HR team.

## Tech stack

- **Java 21** (records, switch expressions, pattern matching, text blocks)
- **Spring Boot 3.5** - REST API, dependency injection, Bean Validation
- **Jakarta Mail** - parses raw MIME emails: folded and encoded headers, multipart bodies, base64 and quoted-printable
- **springdoc-openapi** - interactive Swagger docs at `/docs`
- **Spring Boot Actuator** - `/actuator/health` endpoint
- **JUnit 5, AssertJ, MockMvc** - 59 tests, unit and integration
- **JaCoCo** - coverage report; the build fails below 80% (currently 97%)
- **GitHub Actions** - CI on every push; tagged versions publish a release jar and a Docker image to GitHub Container Registry
- **Docker** - multi-stage build, runs as a non-root user with a health check
- **Frontend** - one dependency-free HTML page: accessible tabs, drag-and-drop, keyboard focus states, reduced-motion support, works on phones

## Architecture

```
Browser / API client
        |
        v
ScanController  (REST layer: routing, validation, RFC 9457 error format)
        |                     \
        v                      RawEmailParser (Jakarta Mail: raw email -> From, Reply-To, body, headers)
ScanService     (runs every rule, sorts findings, computes score)
        |
        v
List<ScanRule>  (each check is its own class, injected by Spring)
   |-- ImpersonationRule --> DomainAnalyzer --> DomainUtils, CompanyDirectory
   |-- SenderMismatchRule
   |-- ReplyToMismatchRule
   |-- EmailAuthenticationRule
   '-- KeywordRule subclasses (sensitive info, payment, off-platform, urgency, too-good-to-be-true)
```

**Adding a new check takes one new class.** Every rule implements `ScanRule` and is a Spring `@Component`, so Spring automatically injects it into `ScanService`. Nothing else changes.

Scoring is deliberately transparent: HIGH = 30 points, MEDIUM = 15, LOW = 5, capped at 100. A score of 50+ is "Likely scam", 20-49 is "Suspicious". Anyone can see why a message got its score.

## API

Interactive docs: http://localhost:8080/docs

### `POST /api/v1/scan/raw` - scan a full email

```bash
curl -X POST localhost:8080/api/v1/scan/raw \
  -H "Content-Type: text/plain" \
  --data-binary @src/main/resources/static/examples/fake-ocient-offer.eml
```

Returns what was read from the email plus the report:

```json
{
  "email": { "from": "\"Ocient Talent Team\" <hiring@ocient-careers.com>", "replyTo": "ocient.onboarding@gmail.com", "subject": "Offer Letter - Junior Software Engineer (Remote)", "body": "...", "headers": "..." },
  "report": {
    "score": 100,
    "verdict": "LIKELY_SCAM",
    "claimedCompany": "Ocient",
    "findings": [
      { "ruleId": "email-auth-dmarc", "severity": "HIGH", "message": "DMARC check returned 'fail'...", "evidence": "dmarc=fail" }
    ]
  }
}
```

### `POST /api/v1/scan` - scan individual fields

```bash
curl -X POST localhost:8080/api/v1/scan \
  -H "Content-Type: application/json" \
  -d '{"sender": "hiring@ocient-careers.com", "body": "Send your SSN within 24 hours."}'
```

| Field | Required | Description |
|---|---|---|
| `sender` | yes | From address |
| `body` | yes | Message text |
| `replyTo` | no | Reply-To address |
| `claimedCompany` | no | Company they claim to represent (detected from the body if blank) |
| `headers` | no | Raw email headers, for SPF/DKIM/DMARC checks |

### `GET /api/v1/domains/check?domain=ocient.co`

Checks a single domain for impersonation.

### `GET /api/v1/companies`

Lists the companies the scanner knows. To add one, edit [`companies.json`](src/main/resources/companies.json); no code changes needed.

Invalid input returns `400` in the standard [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem-details format, with a message that says how to fix it.

## Developing

```bash
mvn spring-boot:run      # run locally on :8080
mvn verify               # tests + coverage gate; report at target/site/jacoco/index.html
docker build -t recruit-guard .
```

Releases are automatic: publishing a GitHub Release (for example `v1.2.0`) builds, tests, attaches the jar to it, and publishes the Docker image.

Want to add a company or a new scam pattern? See [CONTRIBUTING.md](CONTRIBUTING.md). A walkthrough of how everything works is in [docs/HOW_IT_WORKS.md](docs/HOW_IT_WORKS.md).

## What I'd add next

- Look up real domain age through RDAP (scam domains are often days old)
- A browser extension that checks emails right inside Gmail
- Scoring weights tuned against a labeled dataset of real scam reports

## License

MIT
