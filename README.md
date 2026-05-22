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

Authentication uses the `ANTHROPIC_API_KEY` environment variable.
