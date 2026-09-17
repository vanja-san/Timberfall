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

1. Install Fabric API for Minecraft 26.3.
2. Drop the `Timberfall-v*-mc26.3-Fabric.jar` into your `mods/` folder.

## Configuration

Created on first launch at `config/timberfall.json`. All values are
clamped to safe ranges on load, so a hand-edited file cannot break the mod.

## License

CC0-1.0