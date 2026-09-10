# Contributing

Thank you for improving Captcha Service.

1. Discuss substantial behavior changes in an issue first.
2. Create a focused branch and keep commits reviewable.
3. Use Java 17, preserve the two-process boundary, and never add secrets or
   production data.
4. Run `./mvnw test` and `./mvnw package` (or `mvnw.cmd` on Windows).
5. Add tests that use fakes or pure units; CI must not require MongoDB, Redis,
   network access, or a model file.
6. Update README and `docs/` for user-visible changes.
7. Open a pull request describing motivation, behavior, tests, and security impact.

By contributing, you agree that your contribution is licensed under the MIT License.
