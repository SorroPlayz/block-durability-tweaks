# BlockDurabilityTweaks (Paper 1.20.1 / Java 17)

BlockDurabilityTweaks lets you tune how “durable” blocks feel by controlling:
- Mining difficulty (client crack animation + break timing)
- Explosion block loss (best-effort scaling)
- Profiles per-world
- Per-tool multipliers
- Per-biome modifiers
- HP mode (Rust-style block HP + memory)
- GUI profile selector
- Timed profile events
- WorldGuard region → profile auto-binding
- Admin inspect wand
- HP ActionBar display

⚠ Vanilla-by-default: nothing changes unless you enable features or profiles.

---

## Quick Start
1. Drop the jar into /plugins and start the server
2. Edit `plugins/BlockDurabilityTweaks/config.yml`
3. Create profiles in:
   `plugins/BlockDurabilityTweaks/worlds/<world>/profiles/`

Reload with `/bd reload`

---

## Commands
- /bd info
- /bd reload
- /bd inspect
- /bd gui
- /bd world <world> set <profile>
- /bd world <world> reset
- /bd events list|run|stop

---

## Profiles Example
```yml
multipliers:
  hardness: 2.0
  blast_resistance: 1.5

overrides:
  OBSIDIAN:
    hardness: 8.0
    blast_resistance: 3.0
```

---

## WorldGuard Region Binding
```yml
region_profile_binding:
  enabled: true
  worlds:
    world:
      regions:
        spawn: vanilla
        raid_zone: raid_hour
```

---

## HP Mode + ActionBar
```yml
features:
  mode: HP

hp_actionbar:
  enabled: true
  format: "&cHP [&f{hp}&7/&f{max}&c]"
```

---

## Inspect Wand
Use `/bd inspect` then right-click blocks to view:
- Vanilla stats
- Active profile
- Multipliers / HP

Permission:
`blockdurabilitytweaks.inspect`

---

## Notes
Minecraft does not allow changing true block hardness/blast resistance.
This plugin simulates durability via mining control, HP, and explosion filtering.

---

© Sorro / BlockDurabilityTweaks
