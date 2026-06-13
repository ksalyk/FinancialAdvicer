# Third-party agent skills

This project vendors two external, permissively-licensed skill catalogs into
`.claude/`. They are copied (not submoduled) so they version with the repo.

## kotlin-kmp-claude-agent-skills — Apache-2.0
- Source: https://github.com/mmiani/kotlin-kmp-claude-agent-skills
- Author: Mariano Miani
- License: `.claude/licenses/kotlin-kmp-claude-agent-skills-Apache-2.0.txt`
- Installed:
  - 15 skills → `.claude/skills/kotlin-*`
  - Orchestration agents → `.claude/agents/` (planner, implementer, validator, reviewer, fixer)
  - `execute-ticket` command → `.claude/commands/`
  - Validation hooks → `.claude/hooks/` (**adapted** to this project's modules; `validate-detekt.sh` self-skips because detekt is not configured here)
  - Base permissions → `.claude/settings.json`
  - Extended local permissions → `.claude/settings.local.json.example` (NOT applied; your existing `.claude/settings.local.json` was left untouched — merge manually if you want edit/write/push automation)
  - GitHub Actions → `.github/workflows/claude-pr-review.yml`, `claude-pr-fix.yml`
    - **Action required:** add `ANTHROPIC_API_KEY` to the repo's GitHub secrets for the workflows to run.

## compose-skill — MIT
- Source: https://github.com/Meet-Miyani/compose-skill
- Author: Meet Miyani
- License: `.claude/skills/compose-skill/LICENSE`
- Installed: `.claude/skills/compose-skill/` (SKILL.md + agents/ + references/)
- **Local change:** the frontmatter `description` was changed from explicit-only
  invocation to keyword auto-activation (Compose / MVI / Ktor / recomposition / KMP).

## Local skill (not third-party)
- `.claude/skills/scenario-writer/` — authored for this repo's game engine.
