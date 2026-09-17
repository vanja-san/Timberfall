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

Created on first launch at `config/timberfall.json5`. A `timberfall.json` from
an older version is read once and re-saved in the new format. Every setting is
written with a short `//` description above it. Hand-added comments, trailing
commas, single quotes and unquoted keys are accepted on load. Every value is
clamped to a safe range on load, so a hand-edited file cannot break the mod.

There is no in-game configuration screen — a GUI would have to run on the
client, while Timberfall is installed on the server. Edit the file and restart
the server.

## License

CC0-1.0