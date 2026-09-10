# Security Policy

## Supported versions

Security fixes are applied to the latest revision on the default branch.

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability. Use GitHub's private
security advisory workflow for this repository and include reproduction steps,
impact, affected versions, and a proposed mitigation when possible. Please
allow maintainers reasonable time to investigate before public disclosure.

## Operational security

- Never commit `.env`, database credentials, API keys, private keys, CA files,
  model artifacts, uploaded images, or production logs.
- Terminate public HTTPS at a hardened reverse proxy; the embedded Jetty server
  is HTTP only.
- Use Redis TLS. With `REDIS_CA_PATH` unset, the JVM default truststore is used;
  setting it selects one explicit CA. Certificate verification is never disabled.
- Rotate API keys, restrict MongoDB/Redis network access, and run each process
  with least privilege.
- Treat uploaded CAPTCHA images and recognition results as sensitive data and
  define retention/deletion controls appropriate to your jurisdiction.

The current API-key mechanism is application-level authentication, not OAuth,
and does not provide user delegation or scoped tokens.
