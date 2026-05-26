---
description: ARCHITECTURAL STATIC GATE & REJECTION CRITERIA
---

# WORKFLOW: ARCHITECTURAL REVIEW GATE

## §0 — Context Initialization

Before executing any review:
1. Load `.agent/rules/naming_conventions.md`
2. Load `.agent/rules/security_standards.md`
3. Load `.agent/rules/performance_standards.md`
4. Load `.agent/rules/ui_standards.md`

## §1 — Rejection Gates

Immediately halt and reject if any of the following are detected:

### Backend Java Gates

| Gate | Pattern | Remediation |
|:---|:---|:---|
| **Inline Serialization** | Multi-line `ObjectMapper` try-catch in services | Extract to mapper utilities or JPA `@Convert` |
| **Simulated Streaming** | `String.split(" ")` + `Thread.sleep` | Use native `Flux<ChatResponse>` |
| **FQCN Inline** | `java.util.Set<String>` in method bodies | Use imports |
| **Mutation Query Dup** | Write then separate read of same record | Return domain record from write method |
| **Anemic Records [ERR-042]** | Null-checks or defaults in services | Move to record compact constructor |
| **Read-Before-Write [ERR-043]** | `exists*`/`find*` before `save` | Use DB constraints + `DataIntegrityViolationException` |
| **Engine Coupling [ERR-100]** | Engine importing `ToolRegistry`, `McpOrchestrator` | Use interceptor pipeline |
| **N+1 Queries [ERR-200]** | Sequential repo calls in loops | Use `LEFT JOIN FETCH` |
| **Procedural Validation [ERR-103]** | Manual null-checks in services/engines | Move to record constructors |
| **Imperative Noise [ERR-104]** | Nested ternaries, empty catches, obvious comments | Clean up |
| **UI Graph Bypass [ERR-106]** | Hardcoded feature toggles in UI/CLI | Map from `OperationGraph` |
| **Sealed Pattern [ERR-107]** | `instanceof` for `NodeState` | Use switch expression |
| **Pseudo-Records [ERR-109]** | Public class with record-like accessors but no `get*` | Use Java `record` |
| **Encapsulation Leak [ERR-110]** | Public interceptors/filters/converters | Make package-private |
| **Polymorphic Sniffing [ERR-111]** | String regex type detection | Use sealed/discriminated unions |
| **Protocol Mixing [ERR-112]** | REST + GraphQL in flat package | Isolate in sub-packages |
| **Env Sniffing [ERR-113]** | `Environment.getProperty()` in beans | Use typed `@ConfigurationProperties` |
| **Inline Mapping [ERR-114]** | 5+ line setter chains in controllers/services | Extract to `*Mapper` |
| **Prefix Violation [ERR-104]** | `OrasakaEngine`, `OrasakaSidebar` | Remove prefix |
| **Caller-Side Validation [ERR-116]** | `if (request.context() != null)` or `Objects.requireNonNull(dto.field())` in a service | The owning record validates its own fields in its compact constructor. Callers trust the invariant. Favor `record` over `class`. |

### Frontend TypeScript Gates

| Gate | Pattern | Remediation |
|:---|:---|:---|
| **Hardcoded Strings [ERR-115]** | Literal text in JSX/catch/validation | Use `TranslationDictionary` |
| **Date Mutation [ERR-108]** | `setDate()`, `setHours()` on `Date` | Use `date-fns` |
| **File Size** | `.tsx` > 250 lines | Split into sub-components |
| **Inline Loops** | Non-trivial `.map()` JSX | Extract to named component |

### Python Gates

| Gate | Pattern | Remediation |
|:---|:---|:---|
| **No Type Hints** | Missing function signatures or return types | Add PEP 484 type hints |
| **Bare Except** | `except:` without specific exception type | Catch specific exceptions or log explicitly |
| **Hardcoded Secrets** | API keys or tokens in source code | Use env vars (`os.getenv`) |
| **Print in Production** | `print()` statements used for logging | Replace with standard `logging` module |
| **Mutable Defaults** | Banned mutable default arguments `def f(x=[])` | Use `x=None` and initialize inside |
| **Dead Code / Unused Imports** | Unused import statements or variables | Purge unused code |
| **Path Slicing** | Raw string concatenation for directory paths | Use `pathlib.Path` |
| **Dynamic Pip** | Runtime calling of `pip` or package installers | Pre-declare packages in `requirements.txt` |
| **Resource Leak** | Opening files/sockets without `with` context manager | Use context managers for proper resource cleanup |
| **Bad Exit Code** | Python scripts exiting without explicit exit codes | Use `sys.exit(code)` explicitly |


### Terraform Gates

| Gate | Pattern | Remediation |
|:---|:---|:---|
| **Hardcoded Values** | Inline strings in resources | Use `var.*` references |
| **Missing Description** | Variables without `description` | Add description block |
| **No Sensitivity** | Secrets not marked `sensitive` | Add `sensitive = true` |

## §2 — Documentation Gate

- Missing Javadoc on public interfaces/methods → compilation failure.
- Missing TSDoc on shared React components/hooks → review violation.
- Obvious `/** get */` or self-evident comments → must be purged.

## §3 — Code Quality Gates

### Java (Spotless + Google Java Style)
```bash
mvn spotless:apply -pl <module>
mvn spotless:check -pl <module>
```

### Frontend (Prettier + ESLint)
```bash
cd orasaka-ui && npm run format
cd orasaka-ui && npm run lint
```

### Python (Ruff + Black)
For Python workers and E2E testing scripts:
```bash
# Check formatting and linting rules
ruff check scratch/ orasaka-workers/
black --check scratch/ orasaka-workers/

# Auto-format and auto-fix
ruff check --fix scratch/ orasaka-workers/
black scratch/ orasaka-workers/
```

### SonarQube Quality Gate

#### Running Analysis
```bash
# Full backend analysis (requires SONAR_TOKEN env var)
mvn verify sonar:sonar -Dsonar.token=$SONAR_TOKEN

# Single module analysis
mvn verify sonar:sonar -pl orasaka-core -am -Dsonar.token=$SONAR_TOKEN
```

#### Automatic Rejection Rules (SonarQube)

| Rule ID | Category | Description | Severity |
|:---|:---|:---|:---|
| **S3776** | Code Smell | Cognitive Complexity > 15 | Critical |
| **S1068** | Code Smell | Unused private fields | Major |
| **S1144** | Code Smell | Unused private methods | Major |
| **S3655** | Bug | `Optional.get()` without `isPresent()`/`isEmpty()` | Blocker |
| **S5778** | Code Smell | Multiple invocations in `assertThrows` lambda | Major |
| **S1602** | Code Smell | Useless curly braces around lambda | Minor |
| **S1481** | Code Smell | Unused local variables | Major |
| **S106** | Code Smell | `System.out`/`System.err` in production code | Major |
| **S2221** | Code Smell | Broad `catch (Exception e)` in non-adapter code | Major |
| **S1135** | Info | `TODO`/`FIXME` without tracking issue | Info |
| **S2629** | Performance | String concatenation in `logger.info()` | Major |

#### Quality Gate Thresholds

| Metric | Threshold | Action |
|:---|:---|:---|
| New code coverage | ≥ 80% | Block merge |
| Duplicated lines on new code | ≤ 3% | Block merge |
| Reliability rating | A | Block merge |
| Security rating | A | Block merge |
| Maintainability rating | A | Block merge |
| Cognitive Complexity per method | ≤ 15 | Refactor to sub-methods |

#### Acceptable Exceptions

`catch (Exception e)` is acceptable **only** in:
- Adapter-layer MCP client initialization (resilient to external service failures)
- SSE streaming error handlers (must not crash the event loop)
- `@Bean` factory methods that provide fallback behavior

All other broad catches must narrow to specific exception types.

## §4 — Verification Pipeline Order

1. Generate / refactor target files
2. Run formatters (`spotless:apply` or `npm run format`)
3. Run static analysis (`spotless:check` or `npm run lint`)
4. Compile: `mvn clean compile -pl orasaka-gateway -am`
5. Test: `mvn test -q` or `npm test`
6. **SonarCloud Analysis** (publishes reports with branch name):
   ```bash
   # Full monorepo (backend + UI + CLI) — loads SONAR_TOKEN from .env
   ./ops/local/scripts/sonar-publish.sh

   # Individual scopes
   ./ops/local/scripts/sonar-publish.sh backend   # Java modules only
   ./ops/local/scripts/sonar-publish.sh ui         # orasaka-ui only
   ./ops/local/scripts/sonar-publish.sh cli        # orasaka-cli only

   # Or use npm scripts directly (SONAR_TOKEN must be exported)
   cd orasaka-ui && npm run sonar
   cd orasaka-cli && npm run sonar
   ```
7. Quality Gate: verify all metrics pass on [SonarCloud Dashboard](https://sonarcloud.io/organizations/orasaka/projects)

### SonarCloud Token Configuration

The `SONAR_TOKEN` environment variable is **never hardcoded**:
- **Local**: Loaded from `.env` at project root (`SONAR_TOKEN=...`)
- **Maven**: Injected via `${env.SONAR_TOKEN}` in `pom.xml` `<sonar.token>` property
- **npm**: Passed via `$SONAR_TOKEN` in the `sonar` script
- **CI/CD**: Set as a repository secret in GitHub Actions / GitLab CI