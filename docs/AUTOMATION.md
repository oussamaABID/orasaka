# Automation & Local Agent Protocol Specification

> Decoupled background execution worker (`orasaka-workers/external-services`) and the NAT-bypassing Local Agent Protocol.

---

## 1. Architectural Setup

```mermaid
graph TD
    rabbitmq[" RabbitMQ Bus"] -->|Consume Approved Jobs / Events| worker["orasaka-workers/external-services"]
    worker -->|Push Telemetry & Heartbeats| rabbitmq
    worker -->|Trigger Outbound Cloud Actions| external[" Cloud APIs (Jira, Slack, WhatsApp)"]
    worker -->|Quartz + Flyway State| db[" PostgreSQL"]
    
    gateway["orasaka-gateway"] -.->|Reverse Tunnel / SSE| cli["orasaka-cli Agent"]
    rabbitmq -.->|Proxy Tunnel Payload| gateway
```

- **Hexagonal Isolation**: The worker is fully isolated. It has zero dependency on `orasaka-gateway`.
- **Communication Invariant**: Inter-module sync is handled via the RabbitMQ broker.
- **Zero Inbound Ports**: The worker runs in a secure cluster mesh; the local CLI agent connects **outbound** to the gateway to fetch requests.

---

## 2. Quartz Scheduler
Automation scheduling states are persisted in the PostgreSQL database:
- **Quartz config**: `spring.quartz.job-store-type = jdbc` with postgres driver delegate.
- **Flyway Integration**: Flyway migrations are disabled on the worker.

---

## 3. Connectors & Log Stubs
The worker does not integrate with actual cloud APIs or Apache Camel routing. Instead:
- **Connector Dispatcher**: Outbound actions (Jira, Slack, Messenger, WhatsApp) are implemented as log-stubs in `ConnectorDispatcher.java` (`orasaka-apps/orasaka-workers/external-services/`).
- **Execution Logs**: Dispatching acts as a stub simulation logging payload metadata.
- **CLI Agent Routing**: If the type is `CLI_AGENT`, it correctly serializes and dispatches to RabbitMQ under the `cli.{userId}.dispatch` routing key for remote CLI execution.

---

## 4. Local Agent Protocol (Reverse SSE Tunneling)

```mermaid
sequenceDiagram
    participant CLI as orasaka-cli (User)
    participant GW as orasaka-gateway
    participant ID as Identity Core
    participant MQ as RabbitMQ Exchange
    participant WK as Automation Worker

    Note over CLI,GW: Phase 1 — Secure Reverse Registration
    CLI->>GW: GET /api/v1/agent/stream (Bearer Token)
    GW->>ID: Validate JWT
    ID-->>GW: Authenticated (userId)
    GW->>MQ: Publish heartbeat (cli.online)
    GW-->>CLI: SSE Tunnel Established

    Note over GW,WK: Phase 2 — Job Dispatch
    WK->>MQ: Publish CLI Job (orasaka.automation.exchange)
    MQ-->>GW: Consume job (cli.{userId}.dispatch)
    GW-->>CLI: SSE Event: automation_payload

    Note over CLI,GW: Phase 3 — Local Consent & Execution
    CLI->>CLI: Prompt User [Y/n]
    CLI->>CLI: Execute local script safely
    CLI->>GW: POST /api/v1/agent/report (Result JSON)
    GW->>MQ: Publish result (cli.{userId}.result)
    MQ-->>WK: Mark Job COMPLETED
```

- **Phase 1 (Registration)**: The CLI opens an SSE channel (`GET /api/v1/agent/stream`). Gateway registers the channel and broadcasts metadata.
- **Phase 2 (Dispatch)**: The automation worker posts a job payload (containing script and directory limits) to RabbitMQ. The Gateway consumes and proxies it via the SSE stream.
- **Phase 3 (Consent & Execution)**: The CLI agent prompts the user in the terminal (`Execute this action? [Y/n]`). If approved, the command runs and output is reported via `POST /api/v1/agent/report`.

---

## 5. Security & Isolation Matrix

- **NAT Bypassing**: SSE pulls commands outbound; no firewall inbound rules needed.
- **User Verification**: Interactive prompt confirmation `[Y/n]` blocks unapproved command runs.
- **Sandboxing**: Script actions restricted to user-configured directory lists.
- **Transport**: Secured with TLS. Timeout limits (default 300s) evict hung scripts.

---

## 6. RabbitMQ Topology

| Exchange | Type | Routing Key | Queue | Consumer |
| :--- | :--- | :--- | :--- | :--- |
| `orasaka.automation.exchange` | Topic | `job.approved` | `orasaka.automation.jobs` | Worker (`AutomationJobListener`) |
| `orasaka.automation.exchange` | Topic | `cli.{userId}.dispatch` | Per-User Dispatch | Gateway |
| `orasaka.automation.exchange` | Topic | `cli.{userId}.result` | Per-User Result | Worker (`AutomationJobListener`) |
| `orasaka.cli.exchange` | Fanout | `cli.online` | `orasaka.cli.heartbeat` | Gateway |
| `orasaka.identity.events` | Topic | `user.registered` | `orasaka.workers.identity.registration` | Worker (`IdentityNotificationListener`) |
| `orasaka.identity.events` | Topic | `password.reset` | `orasaka.workers.identity.password` | Worker (`PasswordNotificationListener`) |

---

## Related Documentation
- [Developer Onboarding Guide](101.md)
- [Architecture Reference](ARCHITECTURE.md)
- [CLI Reference](CLI.md)
- [ADR Indexes](CONTEXT.md)

