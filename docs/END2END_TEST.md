# Integration & E2E Testing Framework

> Comprehensive testing infrastructure: Unit, Governance (ArchUnit), Integration (Testcontainers), and E2E (Playwright & Vitest).

---

## 1. Test Pyramid Architecture

```
                    ┌──────────────┐
                    │   Playwright  │  ← E2E (UI): Headless Chromium
                    │   Smoke Test  │     BFF SSE streaming mocks
                    └──────┬───────┘
                           │
                   ┌────────┴────────┐
                   │  Vitest + Execa  │  ← E2E (CLI): Temp directory
                   │  CLI Sandbox     │     sandboxed subprocesses
                   └────────┬────────┘
                            │
            ┌───────────────┴───────────────┐
            │   Spring Boot + Testcontainers │  ← Integration: Ephemeral Docker
            │   Singleton Container Pattern   │     @DynamicPropertySource ports
            └───────┬───────────────┬───────┘
                    │               │
       ┌────────────┴────────────┐ ┌┴──────────────────────────────────┐
       │  ArchUnit Governance     │ │ JUnit 5 + Mockito                 │  ← Unit
       │  GovernanceTest.java     │ │ Record constructors, algorithms  │     1:1 matching
       └─────────────────────────┘ └───────────────────────────────────┘
```

| Tier | Framework | Purpose | Context Location |
| :--- | :--- | :--- | :--- |
| **Unit** | JUnit 5 / Mockito | Business algorithms, constructor boundaries | `src/test/java/**/` |
| **Governance** | ArchUnit | Core compile-time decoupled architecture rules | `GovernanceTest.java` |
| **Integration** | Testcontainers | Epoxy PostgreSQL, Redis, RabbitMQ runs | `AbstractContainerIntegrationTest` |
| **E2E (UI)** | Playwright Java | Headless Chromium rendering & SSE responses | `orasaka-apps/orasaka-end2end/src/test/java/` |
| **E2E (CLI)** | Vitest / Execa | Sandbox temp process command execution | `orasaka-apps/orasaka-cli/e2e/` |

---

## 2. Integration Testing (Testcontainers)

All integration suites extend `AbstractContainerIntegrationTest` (starts PostgreSQL, Redis, and RabbitMQ once per JVM process). Dynamic ports are resolved using `@DynamicPropertySource` to eliminate hardcoded defaults.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthControllerIntegrationTest extends AbstractContainerIntegrationTest {
    @LocalServerPort private int port;

    @Test
    void shouldReturnHealthyStatus() {
        assertThat(port).isPositive();
    }
}
```

- Run: `./mvnw clean verify` (Requires Docker).

---

## 3. ArchUnit Governance Rules

Enforced at compile time inside `GovernanceTest.java` (`orasaka-core` checks):
- `noWebControllersInCore`: Zero `@RestController` or Spring web imports.
- `noServletDependenciesInCore`: Zero `jakarta.servlet` leaks.
- `noAmqpDependenciesInCore` / `noRabbitMqDependenciesInCore`: Decouples AMQP/RabbitMQ.
- `recordsMustBePureDataCarriers`: Enforces record purity (no dependencies/IO inside records).

---

## 4. UI E2E Playwright Smoke Tests

- Dev server: Next.js on `http://localhost:3000`.
- Mocking: Intercepts network calls to isolate front-end tests:
  ```typescript
  await page.route('**/api/v1/chat/stream**', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'text/event-stream',
      body: 'data: {"content":"Hello"}\n\ndata: {"content":" World"}\n\n',
    });
  });
  ```
- Run (via Maven): `./mvnw clean verify -P e2e-tests`.
- Run (standalone): `npm run test:e2e --prefix orasaka-apps/orasaka-ui`.

---

## 5. CLI E2E Sandbox Tests

CLI tests run using Vitest inside a temporary operating system folder to avoid polluting the workspace root:
```typescript
import { execa } from 'execa';
const sandbox = fs.mkdtempSync(path.join(os.tmpdir(), 'orasaka-e2e-'));
const result = await execa('npx', ['orasaka', 'init'], { cwd: sandbox });
expect(result.exitCode).toBe(0);
```
- Run (via Maven): `./mvnw clean verify -P e2e-tests`.
- Run (standalone): `npm run test:e2e --prefix orasaka-apps/orasaka-cli`.

---

## 6. CI Pipeline Stages

```
[Phase 1: Lint / Spotless] ──► [Phase 2: Java Testcontainers] ─┬─► [Phase 5: SonarCloud]
                           ──► [Phase 3: JS/TS Unit Tests]   ─┘─► [Phase 4: Playwright E2E]
```

---

## 7. orasaka-end2end Module Architecture (ADR-040)

Dedicated Maven submodule at `orasaka-apps/orasaka-end2end/` housing the complete hermetic E2E testing matrix.

### Sequential Execution Timeline

```
pre-integration-test:
  1. Fabric8 → compose start db-local (PostgreSQL, TCP:5432 wait)
  2. process-exec:start → java -jar orasaka-gateway-*.jar (background)
  3. Antrun → poll /actuator/health until 200 UP (120s timeout)
  4. frontend-maven-plugin → install Node v22

integration-test:
  Tier 1: npx httpyac send src/test/resources/api-contracts/ --all --bail
  Tier 2: npm run test:e2e (orasaka-apps/orasaka-cli)
  Tier 3: Failsafe → *IT.java (Playwright Java, headless Chromium)

post-integration-test:
  process-exec:stop-all → kill gateway process
  Fabric8 → stop + remove db-local

verify:
  Failsafe → verify goal (fail build on test failures)
```

### Test Classes

| Class | Tier | What it validates |
| :--- | :---: | :--- |
| `GatewayHealthIT` | 3 | Actuator health via headless Chromium |
| `ChatSessionIT` | 3 | Full user journey: input → submit → streaming tokens → unlock |

### API Contract Files

Relocated from root `tests/api-contracts/` into `orasaka-apps/orasaka-end2end/src/test/resources/api-contracts/`:
- `health.http` — actuator health probe
- `models.http` — model listing endpoint
- `operations.http` — chat/operations API

---

## Related Documentation
- [Developer Onboarding Guide](101.md)
- [Architecture Reference](ARCHITECTURE.md)
- [ADR Index Log](CONTEXT.md)
