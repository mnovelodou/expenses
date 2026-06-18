# Expenses

> A spending tracker you *talk to* instead of tap through.

Most expense apps die the same death: too much friction to log a purchase, too
much friction to understand where the money went. So you drift back to a
spreadsheet. Expenses is an experiment to remove that friction by putting a
**chat interface** in front of a clean, well-specified API — so logging and
understanding your spending feels like sending a message.

---

## Why another spending tracker?

Yes, this is *another* one. Here's the honest reason: I keep abandoning expense
apps. Every time I try to register a purchase, generate a report, or just
understand my spending, there's enough friction that I give up and crawl back to
my spreadsheet.

The spreadsheet wins because it never gets in my way — but it can't read a
receipt, split a charge, or answer a question. This project is my attempt to get
the best of both: the zero-friction feel of a spreadsheet with the intelligence
of an assistant.

## What's different?

The interface is a **chatbot**, not a sea of forms. The goal is to handle
requests like:

- *"Register this $14 spend as entertainment."*
- *"Read this receipt and create a grocery expense."*
- *"Give me a summary of my spending last month."*
- *"How much have I spent on Amazon?"*
- *"Split this $34.56 expense — $12.20 is Clothes, the rest stays Groceries."*

Under the hood, an agent translates natural language into calls against a typed
REST API with strong validation (e.g. a split's parts must sum back to the
original amount). The chat is the easy part to *use*; the API is the part that
keeps the data correct.

## Tech at a glance

Java 21 · Spring Boot 3.5 · PostgreSQL (plain JDBC, no ORM) · OAuth2 resource
server · Testcontainers for integration tests. Development follows a
**spec-driven workflow** with [OpenSpec](openspec/) — every behavioral change
starts as a written proposal and spec before any code is touched.

See **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** for the full design,
module layout, and the reasoning behind these choices.

## Running locally

You'll need Docker installed.

```bash
# starts Postgres (docker compose) and boots the app with the local profile
./gradlew localRun
```

Stop the database when you're done:

```bash
docker compose down -v
```

## API

The HTTP API covers accounts and expenses (create, update, list, delete,
cursor pagination, criteria search, and amount splitting). Endpoints are
secured with OAuth2 bearer tokens.

Full request/response examples live in **[docs/API.md](docs/API.md)**.

## Roadmap

- **Agent skills** — fully exercise the API through Claude Code and Codex via
  SKILLS, so any agent can drive it end to end.
- **MCP server** — give agents a first-class way to understand the app and use
  the API safely (validating, for example, that a split sums to the original
  amount).
- **Budgets & periods** — set a budget per category over a defined period, and
  generate a report automatically when the period closes.
- **Messaging integrations** — talk to it from WhatsApp or Telegram.

---

*This is a personal project and a work in progress — built in the open as part
of my portfolio.*
