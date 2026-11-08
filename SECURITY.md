# Security policy

See [docs/security/](docs/security/) for the full set:

- [Threat model](docs/security/model.md)
- [Reporting a vulnerability](docs/security/reporting.md)
- [Known limits](docs/security/known-limits.md)
- [Dependency policy](docs/security/dependencies.md)
- [Code signing](docs/security/signing.md)
- [Meta firmware observations](docs/security/firmware-review.md)

## In short

- **Don't file public issues for QuestClassic security bugs.** Use GitHub's private vulnerability reporting on the repo. Response within 72 hours.
- **Meta firmware bugs go to Meta.** [bugbounty.meta.com](https://bugbounty.meta.com/).

## Supported versions

| Version | Security fixes |
|---|---|
| 0.1.x | ✅ (current) |

Once 0.2.0 ships, we'll declare 0.1.x EOL. We support only the latest minor until the project is bigger.

## Fingerprints

Once we start signing releases, the signing key's SHA-256 fingerprint will be published here and in every release note. Until then, releases are debug-signed; verify the APK's `SHA-256` against the one printed on the GitHub release page if in doubt.
