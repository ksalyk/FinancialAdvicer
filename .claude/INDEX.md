# .claude Folder Index

This folder is kept small on purpose. It should contain durable project context for future Claude/Codex sessions, not one-off session summaries, completed phase reports, or stale planning notes.

## Keep

- `PROJECT_OVERVIEW.md` - project description, architecture, modules, build commands, and data flow.
- `EXECUTION_PLAN_DETAILED.md` - detailed implementation plan and task breakdown.
- `SCENARIO_GRAPH_GUIDE.md` - how to write and register scenario graphs.
- `SCENARIO_REFERENCE.md` - scenario writing reference for financial mistakes and scam patterns.
- `TEST_GUIDE.md` - backend test coverage and run guidance.
- `settings.local.json` - local Claude permissions.

## Project Root

Root-level docs stay outside `.claude` because tooling expects them there:

- `CLAUDE.md` - project instructions and architecture.
- `README.md` - public project readme.
- `AGENTS.md` - Codex instructions, when present.

## Cleanup Rule

Do not add generated session reports, temporary phase plans, or completion summaries here. If a note is still useful, fold it into `PROJECT_OVERVIEW.md`, `EXECUTION_PLAN_DETAILED.md`, or the relevant scenario/test guide.
