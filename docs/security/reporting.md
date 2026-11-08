# Reporting a vulnerability

If you believe you've found a security issue in QuestClassic itself (not Meta's firmware — different process, see below):

## Please don't file a public issue

Public issues become CVEs on a timer, and if the vulnerability affects installed-in-the-wild copies we'd rather have a fix ready first.

## Use GitHub's private vulnerability reporting

1. Go to [github.com/A-Hackers-Guide/QuestClassic/security](https://github.com/A-Hackers-Guide/QuestClassic/security).
2. Click **Report a vulnerability**.
3. Fill in what you found, how to reproduce, and what you think the severity is.

GitHub routes it to the maintainers privately. We confirm receipt within 72 hours and coordinate a fix + disclosure timeline from there.

## What's in scope

- Code paths in `app/src/`.
- Our CI + release workflows.
- Our Python tools under `tools/`.
- Our documentation if it would mislead readers into unsafe behavior.

## What's out of scope

- **Meta firmware vulnerabilities.** QuestClassic is not Meta software and we can't patch it. If you find a flaw in Meta's firmware itself, report it to [Meta's bug-bounty program](https://bugbounty.meta.com/). Our [firmware-review.md](firmware-review.md) documents public observations, not undisclosed vulns.
- **Third-party AI backends.** Report to the provider.
- **General Android platform issues.** Report to [Google's VRP](https://bughunters.google.com/).
- **Social engineering tests.** No.

## Our response

- We acknowledge in ≤ 72 hours.
- We investigate; if valid, we draft a fix.
- We coordinate disclosure with you — typical window is 30-90 days depending on severity and complexity.
- We credit you in the release notes and the CVE if one's requested, unless you prefer anonymity.
- We don't offer bug bounties right now (hobby project). If you want paid bug-bounty work, aim at Meta's program — they'll pay for serious Quest findings.

## Severity we care about

- **Critical**: lets a remote attacker run code on the user's headset, exfiltrate their API keys, or impersonate our update channel.
- **High**: silently bypasses our signature verifier (e.g., produces "valid" for a tampered OTA), or lets a malicious site/payload crash the app in a way that leaves state corrupted.
- **Medium**: info leaks beyond what Android's own public APIs expose; denial-of-service in normal use.
- **Low**: anything that requires physical access + active cooperation of the user.

---

_Last reviewed against v0.1.0._
