# Agent Skills

## Provenance

- Source: `Jahrome907/minecraft-agent-skills`
- Branch: `main`
- Locked commit: `82c38979adcbf6244c4ee835e26c3460ea1f97e4`
- Installed at: `2026-07-15T09:11:03Z`
- Scope: project-local `.agents/skills`

The authoritative provenance record is [`.agents/skills.lock.json`](../.agents/skills.lock.json).

## Installed Skills

- `minecraft-modding`: loader-specific mod code, registration, events, networking, and DataGen.
- `minecraft-testing`: unit, integration, GameTest, regression, and CI test guidance.
- `minecraft-resource-pack`: resource models, textures, sounds, fonts, metadata, and validation.
- `minecraft-ci-release`: build/release automation and publication governance; publishing stays disabled until approved.
- `minecraft-multiloader`: shared Fabric/NeoForge design only when dual-loader support is explicitly selected.

## Updating

```bash
./scripts/update-agent-skills.sh --check
./scripts/update-agent-skills.sh --update
```

The updater resolves a full `main` commit, downloads that pinned archive to a temporary directory, performs front-matter and basic static safety checks, replaces only these five directories, updates the lock, and removes the temporary directory. Review upstream changes before `--update` in security-sensitive environments.

Skills contain instructions and helper resources. They do not replace authoritative Minecraft, Fabric, NeoForge, Gradle, or dependency documentation. Newly installed project Skills are normally discovered by a new Codex session; restart Codex or reopen the project if the current UI does not show them immediately.
