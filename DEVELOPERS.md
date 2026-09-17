# DEVELOPERS.md

## Requirements

- JDK 25+ (Loom 1.18.1 will not run on older JVMs)
- Minecraft 26.3, Fabric Loader 0.19.5, Fabric API

## Project layout

```
src/main/java   shared (common) code and server logic
src/client/java client-only code (enforced by split source sets)
src/main/resources
    fabric.mod.json          mod metadata (version injected from Gradle)
    timberfall.mixins.json   common mixins
    timberfall.client.mixins.json
```

## Key modules

- `chop/ChopManager` – queues one chop per player and advances it a few
  logs per server tick.
- `chop/ChopPlanner` – deterministic tree detection bounded by limits.
- `leaf/LeafDecayEngine` – recomputes leaf distances with two multi-source
  BFS runs; leaf drops are rate-limited to `leafDecayPerTick` per tick.
- `config/Config` – plain settings holder; `config/ConfigManager` loads/saves
  `config/timberfall.json5` through `config/Json5`, writing one `@Comment`
  description above each setting, then clamps every value via `sanitize()`.

## Build

Minecraft 26.3 ships unobfuscated, so no mappings are configured and
Loom 1.18.1 produces no `remap` tasks — the plain `jar` task is the release
artifact. Dependencies are declared with plain `implementation`.

```bash
./gradlew build
```

The jar is named `build/libs/Timberfall-v<version>-mc<mc>-Fabric.jar`
(see the `jar` block in `build.gradle`). Bump `mod_version` in
`gradle.properties` before releasing.

## Development

- Compile both source sets: `./gradlew compileJava compileClientJava`
- Run task naming and no mappings: check MC names directly against the
  unobfuscated 26.3 jar (via Loom caches).
- CI builds on every push/PR in `.github/workflows/build.yml`.

## Conventions

- No logging in the codebase; report failures by returning/ignoring.
- Keep per-tick work bounded (chop batches, BFS caps, decay rate limit).
- New gameplay knobs go into `Config` with a `@Comment` description and a
  `sanitize()` clamp.