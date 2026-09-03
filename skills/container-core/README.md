# container-core Skill

An [Agent Skill](https://agentskills.io) that teaches AI coding agents
(Claude Code, and any other agent supporting the SKILL.md format) how to
use the [Container library](../../README.md) (`com.elveum:container`)
without reading its sources: dependency setup, the full public API
imports reference, and architecture patterns for `Container`, subjects,
pagination, reducers, and testing.

## Installation

**Claude Code (per project):** copy this directory into your project:

```bash
mkdir -p .claude/skills
cp -r skills/container-core .claude/skills/
```

**Claude Code (all projects):** copy it to your user skills directory:

```bash
mkdir -p ~/.claude/skills
cp -r skills/container-core ~/.claude/skills/
```

Or download directly from GitHub without cloning:

```bash
mkdir -p .claude/skills && cd .claude/skills
curl -L https://github.com/romychab/container/archive/refs/heads/main.tar.gz \
  | tar xz --strip-components=2 "container-main/skills/container-core"
```

**Other agents:** place the `container-core` directory wherever your agent
discovers skills (e.g. `~/.agents/skills/` for Codex), or paste
`SKILL.md` + `references/*.md` into the agent's context.

## Contents

| File | Purpose |
|------|---------|
| `SKILL.md` | Entry point: when to use, dependency setup, reference routing table, quick reference |
| `references/container-type.md` | `Container<T>` states, value extraction, transformations, flow extensions, combining flows |
| `references/subjects.md` | `LazyFlowSubject`, `LazyCache`, metadata, source types |
| `references/paging.md` | `pageLoader`, next-page states, pull-to-refresh, flow dependencies, per-item updates |
| `references/reducers.md` | `Reducer`, `ContainerReducer`, `toReducer`, `combineToReducer`, `ReducerOwner` |
| `references/patterns.md` | Architecture patterns across app layers, `LoaderDecorator`, DI, scoping |
| `references/testing.md` | Testing `Container`-based code |

The skill matches library version `3.6.0`.
