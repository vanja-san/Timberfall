# Timberfall

Efficiently fell entire trees with a single chop. Fully configurable,
tick-safe, and stable.

- **Loader:** Fabric
- **Minecraft:** 26.3
- **Java:** 25+
- **Dependencies:** Fabric API

## Features

- Chop a whole tree by breaking one log, executed in small batches per tick
  - Chain mode (default): logs break one by one with particles and sound,
    rippling outward from the chopped block like a chain reaction
  - Instant mode: the whole tree disappears in per-tick batches
  - Optional constant break speed: same felling time regardless of tree size
    or axe material
- Auto-plants a matching sapling where the trunk stood — 2x2 trunks
  (jungle, dark oak, giant spruce) get a full 2x2 patch of saplings
- Modded trees are replanted too: the sapling for any log that no vanilla
  wood family tag covers is discovered from the tree features its sapling can
  grow, so mod trees work without any per-mod configuration
- Accelerated leaf decay with rate-limited drops
- Every gameplay knob is configurable at runtime

## Installation

The mod installs on the server — either a dedicated server or the
integrated server of single-player. Put the
`Timberfall-v*-mc26.3-Fabric.jar` into the server's `mods/` folder
together with Fabric API for Minecraft 26.3.

## Configuration

Created on first launch at `config/timberfall.json5`. A `timberfall.json` from
an older version is read once and migrated to the new format. Every setting is
written with a short `//` description above it, and a clean file is never
overwritten on restart — your hand-added comments and formatting survive.
When a mod update adds new settings, the file is rebuilt exactly once (your
chosen values are kept) so the new options appear. Hand-added comments,
trailing commas, single quotes and unquoted keys are accepted on load. Every
value is clamped to a safe range on load, so a hand-edited file cannot break
the mod. If the file is unreadable, the mod falls back to defaults and keeps
the broken file as `timberfall.json5.corrupt` instead of deleting it.

`chainBreaking` controls how a felled tree disappears: with it enabled (the
default) each log breaks just after the one before it, starting at the block
you chopped — a visible chain reaction up the trunk. Disable it for the
instant per-tick removal.

`constantChopSpeed` (off by default) makes the first block take the same
fixed time no matter how the tree is sized or which axe grade you hold —
big trees and weak tools stop slowing the break down.

There is no in-game configuration screen — a GUI would have to run on the
client, while Timberfall is installed on the server. Edit the file and restart
the server.

## License

CC0-1.0