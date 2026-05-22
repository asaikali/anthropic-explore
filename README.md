# anthropic-explore

A learning sandbox for exploring the Anthropic API for Claude — with a focus on
**Claude Managed Agents** and other platform features as they come up.

The goal is to understand how the API works, not to build a product. Code here
is intentionally small and direct: each endpoint or script demonstrates one
feature so it is easy to read, run, and reason about.

Expect a mix of approaches:

- **Java SDK** — Spring Boot app using the [`anthropic-java`](https://github.com/anthropics/anthropic-sdk-java)
  client. See the controllers under `src/main/java/com/example/` for examples
  (e.g. `RootController` for the Messages API, `AgentController` for the
  Managed Agents beta).
- **Shell scripts** — `curl` against the API directly when that is the
  clearest way to show what a request and response look like.
  Implemented as [mise](https://mise.jdx.dev) tasks under `mise-tasks/`.

## Setup

1. Install the toolchain (Temurin Java 25) declared in `mise.toml`:
   ```
   mise install
   ```
2. Copy the env template and fill in your three credentials:
   ```
   cp .env.example .env
   ```
   - `ENV_KEY` — environment OAuth token from Console (`sk-ant-oat01-…`).
     Used for worker calls.
   - `ENV_ID`  — the self-hosted environment id you created (`env_…`).
   - `API_KEY` — your org API key (`sk-ant-api03-…`). Used for org/admin
     calls (queue stats, agent CRUD, Messages API).
3. `cd` into the repo so mise loads the env. First time only:
   ```
   mise trust
   ```
4. Sanity-check that all four env vars are loaded:
   ```
   mise run env-check
   ```

## Self-hosted environment worker walkthrough

The `mise-tasks/` directory contains one curl-per-endpoint task that mirrors
the worker protocol for self-hosted Managed Agents environments. Walk through
them in order to see the wire-level conversation.

Every task prints the literal `curl` invocation on stderr (`set -x`) so you
can see exactly what is being sent. JSON response goes to stdout. Pipe
`2>/dev/null` to silence the trace.

| Step | Command                                                  | What happens                                                                                                                 | Auth      |
|------|----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|-----------|
| 1    | `mise run stats`                                         | Inspect the queue (`depth`, `pending`, `workers_polling`). Safe to run anytime — does not consume work.                      | `API_KEY` |
| 2    | *(create a session)*                                     | From the Console, the Java agent app, or another curl, create a session against `ENV_ID`. This enqueues a work item.         | —         |
| 3    | `mise run poll [worker_id] [block_ms]`                   | Long-poll the queue. On success, response contains `id` (work_id) and `data.id` (session_id). Capture both — you need them.  | `ENV_KEY` |
| 4    | `mise run ack <work_id>`                                 | Acknowledge the work item: state transitions `queued` → `starting`. Without this nothing else happens.                       | `ENV_KEY` |
| 5    | `mise run heartbeat <work_id>`                           | First call defaults `expected_last_heartbeat=NO_HEARTBEAT` to claim the lease. Subsequent calls must echo the previous `last_heartbeat` value (arg 2) or you get `412 Precondition Failed`. Repeat every ~15s in another terminal to keep the lease alive. | `ENV_KEY` |
| 6a   | `mise run events <session_id>`                           | List events for the session (paginated). Filter with the optional `types` arg, e.g. `mise run events $S asc 50 agent.tool_use`. | `ENV_KEY` |
| 6b   | `mise run tool-result <session_id> <tool_use_id> "<text>"` | When an `agent.tool_use` event appears, do the work locally and POST a `user.tool_result` event back into the session.       | `ENV_KEY` |
| 7    | `mise run stop <work_id>`                                | Stop the work item. Defaults to `force=true`; pass `false` as arg 2 for graceful shutdown.                                   | `ENV_KEY` |

### Suggested first walk-through

Open three terminals (all `cd`-ed into the repo so mise loads the env):

- **Terminal A** — poll once and start the lifecycle:
  ```
  mise run stats                          # see depth=0
  # ... trigger a session against ENV_ID in another window ...
  mise run poll                           # capture work_id (W) and session_id (S)
  mise run ack <W>
  mise run heartbeat <W>                  # note the response's last_heartbeat (LH)
  ```
- **Terminal B** — observe events as the agent runs:
  ```
  mise run events <S>                     # poll periodically, or try `stream`
  ```
- **Terminal C** — keep the lease alive while you experiment:
  ```
  while sleep 15; do
    mise run heartbeat <W> <LH>           # update LH from each response
  done
  ```

When done:
```
mise run stop <W>
```

### Notes

- **Auth split**: org-scoped endpoints (queue stats, agent CRUD, Messages API)
  use the `sk-ant-api03-…` org key via `x-api-key`. Worker-scoped endpoints
  (poll, ack, heartbeat, stop, session events) use the `sk-ant-oat01-…` OAuth
  token via `Authorization: Bearer`. Sending an OAuth token via `x-api-key`
  fails with `Missing Authorization header`.
- **`stream` is experimental**: the public API reference does not document an
  SSE endpoint for session events. The task tries `Accept: text/event-stream`
  against the documented `GET /v1/sessions/{id}/events` endpoint; if the
  server returns plain JSON instead of `data: …` frames, fall back to polling
  `events`.
- **The Java side** (`RootController`, `AgentController`) authenticates with
  `ANTHROPIC_API_KEY` via `AnthropicOkHttpClient.fromEnv()`. That variable is
  separate from the four mise/dotenv vars above — set it in your shell (or
  add it to `.env`) before running the Spring Boot app.
