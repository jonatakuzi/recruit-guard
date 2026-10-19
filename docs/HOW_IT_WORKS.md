# How Recruit Guard Works (interview walkthrough)

This is the guide to explaining the project out loud. If you can talk through every section here, you can handle any question an interviewer asks about it.

---

## The 30-second pitch

> "During my job search I got an email from a fake Ocient recruiter on a look-alike domain. I caught it by checking the domain myself, and realized the checks I was doing by hand could be automated. So I built Recruit Guard: a Spring Boot REST API that takes a recruiter's email and scores it 0 to 100 for scam risk. It detects look-alike domains using edit distance and character normalization, flags requests for things like your SSN, reads the email's SPF, DKIM, and DMARC results, and explains every point of the score. It has 49 tests at 97% coverage, a CI pipeline in GitHub Actions, and runs in Docker."

---

## What happens when you click "Scan"

1. **The browser** sends JSON to `POST /api/v1/scan`.
2. **`ScanController`** receives it. `@Valid` checks the fields (sender and body can't be blank, length limits). Bad input gets a clean `400` from `ApiExceptionHandler`.
3. **`ScanService.scan()`** figures out which company the email claims to be from: the one the user typed, or else the first known company name found in the body.
4. It builds a **`ScanContext`**: the request plus pre-parsed values (sender domain, reply-to domain, lowercased body) so each rule doesn't redo that work.
5. It loops over **every `ScanRule`**, collects all the `Finding`s, sorts them most-severe-first, and adds up the points (capped at 100).
6. The score maps to a **`Verdict`**: 50+ is LIKELY_SCAM, 20-49 SUSPICIOUS, under 20 LOW_RISK.
7. Spring turns the `ScanReport` record into JSON automatically.

---

## The interesting algorithms

### 1. Edit distance (Levenshtein) - `DomainUtils.editDistance`

**Question it answers:** how many single-character changes turn `ocint` into `ocient`? (Answer: 1.)

**How it works:** dynamic programming. Picture a grid with one word across the top and the other down the side. Each cell holds the cheapest way to turn the first *i* letters of one word into the first *j* letters of the other. Each cell is the minimum of:
- the cell to the left + 1 (insert a letter)
- the cell above + 1 (delete a letter)
- the diagonal cell + 0 if the letters match, or + 1 if not (substitute)

The bottom-right cell is the answer.

**Optimization worth mentioning:** each row only depends on the row above it, so I keep two rows instead of the whole grid. That's **O(n x m) time but only O(m) space.**

**Why the 5-letter minimum:** short names like `ngc` are 1-2 edits from tons of unrelated words (`nbc`, `ngo`), so typo checks on them would flag everything. That's a false-positive trade-off I made on purpose.

### 2. Look-alike normalization - `DomainUtils.normalizeLookalikes`

Scammers swap characters that look the same: `0` for `o`, `1` for `l`, `rn` for `m` (r + n looks like m at a glance). I map those back, so `rnicrosoft` becomes `microsoft`, then compare to the real name. It's a hash-map lookup per character, so it runs in linear time.

### 3. Registrable domain - `DomainUtils.registrableDomain`

The part of a domain someone actually *owns* is the last two labels (`ocient.com`), or three for suffixes like `co.uk`. This is what catches the subdomain trick: `ocient.com.hr-portal.net` *looks* like Ocient, but the owner is `hr-portal.net`.

**Honest limitation:** a production version would use the full Public Suffix List. I hard-coded the common two-part suffixes to keep it dependency-free.

---

## Design decisions (the "why" questions)

**"Why is each rule its own class?"**
So adding a check never touches existing code. Every rule implements the `ScanRule` interface and is a Spring `@Component`. Spring finds all of them and injects them into `ScanService` as a `List<ScanRule>`. That's the **Open/Closed principle**: open for extension, closed for modification.

**"What's KeywordRule?"**
An abstract base class for phrase-matching rules. It does the regex matching; subclasses just pass in their patterns, severity, and message. That's the **Template Method pattern**, and it's why each keyword rule is only a few lines.

**"Why records?"**
`Finding`, `ScanRequest`, `ScanReport`, and `ScanContext` are just data. Java records give immutable fields, `equals`, `hashCode`, and `toString` for free, and Jackson converts them to and from JSON with no extra code.

**"Why a simple point system instead of machine learning?"**
Explainability. Someone deciding whether to trust a recruiter needs to know *why* it's flagged. Every point traces back to a specific finding with evidence. And I didn't have a labeled dataset to train on; that's listed as a next step.

**"Why constructor injection?"**
Fields can be `final`, the class can't exist in a half-built state, and tests can construct it directly without Spring (see `DomainAnalyzerTest`).

**"Why put companies in JSON?"**
Adding a company is a data change, not a code change. No recompiling logic.

---

## Testing strategy

- **Unit tests** (`DomainUtilsTest`, `DomainAnalyzerTest`, `EmailAuthenticationRuleTest`): no Spring, very fast. Parameterized tests (`@CsvSource`) run one test method against many inputs.
- **Integration tests** (`ScanServiceTest`): boots the real Spring context and scans realistic scam and legitimate emails end-to-end.
- **Web tests** (`ScanControllerTest`): `MockMvc` sends real HTTP requests to the controllers and checks status codes and JSON, including the 400 error cases.
- **Negative tests matter too:** I test that real domains like `careers.ocient.com` and unrelated ones are *not* flagged. A scanner that flags everything is useless.
- **JaCoCo** fails the build if line coverage drops below 80%. It's at 97%.

---

## DevOps pieces

- **GitHub Actions** (`.github/workflows/ci.yml`): every push runs `mvn verify` (compile, test, coverage gate), uploads the coverage report, then builds the Docker image.
- **Dockerfile:** multi-stage. Stage 1 uses a full Maven image to build the jar; stage 2 copies only the jar into a small JRE image. Smaller image, no build tools in production. It runs as a **non-root user** (a security best practice) and has a `HEALTHCHECK` that hits `/actuator/health`.

---

## Questions to be ready for

| Question | Short answer |
|---|---|
| What was the hardest part? | Tuning false positives. Typo detection on short names flagged everything, so I added the minimum-length rule and tests to prove real domains pass. |
| What's the time complexity of a scan? | Linear in message length for the regex rules, plus O(companies x name length squared) for edit distance. Both tiny in practice. |
| How would you scale it? | It's stateless, so you can run many copies behind a load balancer. Compiled regex patterns are reused across requests. |
| What would you improve? | WHOIS/RDAP domain-age lookups, the full Public Suffix List, `.eml` uploads, and weights tuned on real scam data. |
| How do you know it works? | 49 automated tests covering scam and legitimate emails, run in CI on every push. |
| Could a scammer get around it? | Yes. A brand-new domain with no brand name in it and a clean message would score low. That's why the verdict always says to verify on the company's real careers site, and why domain age is my next feature. |
