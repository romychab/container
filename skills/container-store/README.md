# container-store Skill

An [Agent Skill](https://agentskills.io) that teaches AI coding agents
(Claude Code, and any other agent supporting the SKILL.md format) how to
use the [Store library](../../store/README.md) (`com.elveum:store`)
without reading its sources: dependency setup, the full public API
imports reference, and architecture patterns for every layer of an
Android app (data sources → repositories → ViewModels → Compose screens).

## Installation

**Claude Code (per project):** copy this directory into your project:

```bash
mkdir -p .claude/skills
cp -r skills/container-store .claude/skills/
```

**Claude Code (all projects):** copy it to your user skills directory:

```bash
mkdir -p ~/.claude/skills
cp -r skills/container-store ~/.claude/skills/
```

Or download directly from GitHub without cloning:

```bash
mkdir -p .claude/skills && cd .claude/skills
curl -L https://github.com/romychab/container/archive/refs/heads/main.tar.gz \
  | tar xz --strip-components=2 "container-main/skills/container-store"
```

**Other agents:** place the `container-store` directory wherever your agent
discovers skills (e.g. `~/.agents/skills/` for Codex), or paste
`SKILL.md` + `references/*.md` into the agent's context.

## Contents

| File | Purpose |
|------|---------|
| `SKILL.md` | Entry point: when to use, dependency setup, store-type decision table, quick reference |
| `references/api.md` | Every public import, `StoreResult` / `LoadRequest` semantics, typical operations |
| `references/patterns.md` | Layer-by-layer patterns: data sources, repositories, ViewModels, Compose, DI, scoping |

The skill matches library version `3.1.1`.
