# Security

## Reporting a vulnerability

This is a personal project. If you find a security issue, please open an issue or
contact the maintainer directly rather than disclosing it publicly.

## How secrets are handled

No production secrets live in this repository. Sensitive configuration is supplied
at runtime through environment variables, not committed files:

- **OAuth issuer** — `spring.security.oauth2.resourceserver.jwt.issuer-uri` reads
  from the `OAUTH_ISSUER_URI` environment variable (empty by default). The service
  is a resource server only: it validates JWTs against the provider's published
  keys and never issues, stores, or signs production tokens.
- **Database credentials (production)** — supplied via Spring's standard
  `SPRING_DATASOURCE_*` environment variables. The production profile contains no
  inline URL, username, or password.

## About the committed values

Some non-secret, local-only values are intentionally committed so the project runs
out of the box:

- **`src/test/resources/local/test-rsa-*.pem`** — a throwaway RSA key pair used
  **only** in local and integration tests to mint and validate JWTs against an
  in-memory/test setup. It is **not** a production signing key and has no bearing
  on deployed environments, which validate tokens via the configured OAuth
  provider's JWKS. It is safe to commit, but is called out here so automated
  secret scanners and reviewers understand its scope.
- **Local Postgres credentials** (`postgres`/`postgres` in
  `application-local.properties` and `docker-compose.yaml`) — disposable
  credentials for a local development container. They are not used in any
  deployed environment.
