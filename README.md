# Timberfall

Efficiently fell entire trees with a single chop. Fully configurable,
tick-safe, and stable.

- **Loader:** Fabric
- **Minecraft:** 26.3
- **Java:** 25+
- **Dependencies:** Fabric API

## Features

- Chop a whole tree by breaking one log, executed in small batches per tick
- Auto-plants a matching sapling where the trunk stood
- Accelerated leaf decay with rate-limited drops
- Every gameplay knob is configurable at runtime

## Installation

The mod installs on the server — either a dedicated server or the
integrated server of single-player. Put the
`Timberfall-v*-mc26.3-Fabric.jar` into the server's `mods/` folder
together with Fabric API for Minecraft 26.3.

## Configuration

Created on first launch at `config/timberfall.json`. All values are
clamped to safe ranges on load, so a hand-edited file cannot break the mod.

## License

CC0-1.0