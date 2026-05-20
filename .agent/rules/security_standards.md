# Orasaka Security Standards

## Zero Hardcoded Credentials

- **Keys**: Never hardcode API keys or sensitive endpoints.
- **Binding**: Use Spring-compatible property binding for `CorsProperties`.
- **Injection**: Favor constructor injection for security and testability.

## Environment Isolation

- Ensure the `cors` module does not leak sensitive information via logging.
- Use `masked` values for sensitive extra options where possible.
