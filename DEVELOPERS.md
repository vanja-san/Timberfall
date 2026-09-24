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
  logs per server tick. A chop whose owner logs off, dies or turns into a
  spectator still finishes: remaining logs are dropped plainly, without tool
  damage. `chainBreaking` (config) selects one-log-per-tick removal in the
  planner's breadth-first scan order (chain reaction) or the configured batch
  size with a top-down trunk order (instant). `constantChopSpeed` short-circuits
  the connected-log penalty and the vanilla axe material to a fixed
  per-block break time.
- `chop/ChopPlanner` – deterministic tree detection bounded by limits.
- `util/SaplingUtil` – resolves the replant sapling for a log: the vanilla
  per-family item tags first, then `util/ModTreeSaplingMap`. That map is
  built lazily once per game: it walks every `SaplingBlock` in the block
  registry, reads its grower's feature lists (via the `SaplingBlockMixin`
  and `TreeGrowerMixin` accessors) and resolves each feature key in the
  `worldgen/feature` registry, reading the trunk block the feature places.
  This makes modded trees replantable without any per-mod configuration.
  MC 26.3 has no `ConfiguredFeature`/`TreeConfiguration` anymore – tree
  features are plain `TreeFeature`/`FallenTreeFeature` instances exposing
  their `trunkProvider` publicly.
- `leaf/LeafDecayEngine` – recomputes leaf distances with two multi-source
  BFS runs; leaf drops are rate-limited to `leafDecayPerTick` per tick.
- `config/Config` – plain settings holder; `config/ConfigManager` loads/saves
  `config/timberfall.json5` through `config/Json5`, writing one `@Comment`
  description above each setting, then clamps every value via `sanitize()`.
  A clean up-to-date file is never rewritten; corrupt files are quarantined
  to `timberfall.json5.corrupt`. A schema marker in the header lets the mod
  rebuild the file exactly once after an update adds new settings – bump
  `ConfigManager.SCHEMA_VERSION` whenever `Config` grows new fields.

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

## Tests

Unit tests live in `src/test/java` (JUnit 5, plain JVM — no Fabric runtime).
They cover the pure config pipeline: `Json5` normalisation,
`Config.sanitize()` clamping and `ConfigManager.render()` round-tripping
through the real persistence format. New config-related logic should get a
test here.

```bash
./gradlew test
```

## Development

- Compile both source sets: `./gradlew compileJava compileClientJava`
- Run task naming and no mappings: check MC names directly against the
  unobfuscated 26.3 jar (via Loom caches).
- CI builds and runs tests on every push/PR in `.github/workflows/build.yml`.

## Conventions

- No logging in the codebase; report failures by returning/ignoring.
- Keep per-tick work bounded (chop batches, BFS caps, decay rate limit).
- New gameplay knobs go into `Config` with a `@Comment` description and a
  `sanitize()` clamp.