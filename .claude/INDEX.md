# .claude Folder Index

This folder is kept small on purpose. It should contain durable project context for future Claude/Codex sessions, not one-off session summaries, completed phase reports, or stale planning notes.

## Keep

- `PROJECT_OVERVIEW.md` - project description, architecture, modules, build commands, and data flow.
- `EXECUTION_PLAN_DETAILED.md` - detailed implementation plan and task breakdown (with Codex review notes at the tail).
- `SCENARIO_GRAPH_GUIDE.md` - how to write and register scenario graphs.
- `SCENARIO_REFERENCE.md` - scenario writing reference for financial mistakes and scam patterns.
- `TEST_GUIDE.md` - backend test coverage and run guidance.
- `AdminPanelImpl.md` - admin panel hand-off spec (6 steps, do not re-litigate decisions).
- `market_analysis_2026.md` - April 2026 competitive analysis.
- `settings.local.json` - local Claude permissions.
- `INDEX.md` - this file.

## Project Root

Root-level docs stay outside `.claude` because tooling expects them there:

- `CLAUDE.md` - project instructions and architecture (symlinked to `AGENTS.md`).
- `README.md` - public project readme.
- `AGENTS.md` - Codex instructions, when present.

## Cleanup Rule

Do not add generated session reports, temporary phase plans, or completion summaries here. If a note is still useful, fold it into `PROJECT_OVERVIEW.md`, `EXECUTION_PLAN_DETAILED.md`, or the relevant scenario/test guide.

If a one-off report is already at the project root (e.g. `FIX_REPORT_*.md`) and not referenced by an active task, move it out of root — it belongs in an archive folder, not next to `README.md`.
