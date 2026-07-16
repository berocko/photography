# Release

Publishing is disabled. `.github/workflows/release.yml` is a manually triggered build-only readiness check with read-only repository permission; it does not reference publication tokens or create releases.

No Modrinth, CurseForge, or GitHub Release may be created until the owner approves the project identity, migration policy, versioning, artifact names, target platforms, project IDs, secret names, changelog, license review, and rollback procedure. Do not invent IDs or submit tokens.

The retained upstream build contains publication plugin configuration for Camerapture, but no local command may invoke `publishMods`. Milestone 9 will separate build verification from any future publishing workflow.

