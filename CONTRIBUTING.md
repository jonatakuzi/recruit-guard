# Contributing to Recruit Guard

Thanks for helping job seekers spot scams. Two kinds of contributions are especially useful.

## Add a company

Edit [`src/main/resources/companies.json`](src/main/resources/companies.json) and add the company with **every domain its recruiters really email from**:

```json
"Acme Corp": ["acme.com", "acmecareers.com"]
```

Only add domains you can verify (the company's careers page, or an email you know is real). A wrong entry would mark scams as official.

## Add a scam pattern

Phrase checks live in [`KeywordRules.java`](src/main/java/com/jonatakuzi/recruitguard/rules/KeywordRules.java). Add a regex to an existing rule, or create a new nested class:

```java
@Component
public static class MyNewRule extends KeywordRule {
    public MyNewRule() {
        super("my-rule-id", Severity.MEDIUM,
                "Plain-English explanation shown to the user.",
                List.of("phrase one", "phrase (two|2)"));
    }
}
```

Spring picks it up automatically. Then add a test in `ScanServiceTest` showing it flags a scam **and** doesn't flag a normal recruiter email.

## Report a scam that wasn't caught

Open an issue with the suspicious parts of the email. **Remove your own name, address, and contact details first.**

## Before opening a pull request

```bash
mvn verify
```

All tests must pass and coverage must stay above 80%.
