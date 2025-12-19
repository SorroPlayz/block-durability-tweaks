# BlockDurabilityTweaks (Paper/Spigot/Mohist 1.20.1)

This plugin simulates configurable **block hardness** (mining time) and **blast resistance** (explosion block survival)
using a per-world **profile system**.

## Key points

- **Vanilla by default**: unless you load a profile that changes values, nothing changes.
- Per-world settings are stored as **named profiles** (world saves).
- A per-world `vanilla.yml` reference file is generated that lists **every block** with best-effort vanilla values.
  - Values are read via reflection; if your server build doesn't expose them, they will appear as `NaN`.

## Folder layout (generated)

```
plugins/BlockDurabilityTweaks/
  config.yml
  README-GENERATED.txt
  worlds/
    <world>/
      vanilla.yml
      profiles/
        vanilla.yml
        <your_profile>.yml
```

## Commands

All commands are under:

- `/blockdurability ...`

### General
- `/blockdurability info`
  - Look at a block and shows:
    - Active profile for the world
    - Vanilla hardness/blast (best-effort)
    - Effective hardness/blast from the active profile

- `/blockdurability reload`
  - Reloads `config.yml` and all world profiles.

### World / Profile management
- `/blockdurability world list [world]`
  - Lists available profiles for a world (defaults to your current world if in-game).

- `/blockdurability world load <world> <profile>`
  - Loads and activates a profile for that world (in-memory). (The active profile name is stored in config.yml.)

- `/blockdurability world save <world> <profile>`
  - Saves the currently active profile settings out to a named profile file.

- `/blockdurability world reset <world>`
  - Resets world back to `vanilla` profile.

- `/blockdurability world vanilla <world>`
  - Regenerates the world `vanilla.yml` reference dump.

## Config structure (profiles)

Profiles are stored in:
`plugins/BlockDurabilityTweaks/worlds/<world>/profiles/<profile>.yml`

Example:

```yml
info:
  name: "rust_hardcore"
  description: >
    This setting multiplies all blocks stats unless a block override exists.

multipliers:
  hardness: 2.0
  blast_resistance: 1.0

overrides:
  OBSIDIAN:
    hardness: 5.0
    blast_resistance: 2.0
  STONE:
    hardness: 3.0
```

Notes:
- Use Bukkit Material names (UPPERCASE): `STONE`, `OAK_PLANKS`, `DEEPSLATE`, etc.
- The mining controller only *slows* mining (effective hardness > 1.0). Values <= 1.0 behave vanilla.

## Permissions
- `blockdurabilitytweaks.admin` — access to reload/world commands
- `blockdurabilitytweaks.worldguard.bypass` — bypass WorldGuard check for mining controller
