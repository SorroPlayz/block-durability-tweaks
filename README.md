# BlockDurabilityTweaks (1.20.1) - Stage 1+2

This update adds:
- Stage 1: config toggles foundation (future-proofed for HP mode, GUI, events)
- Stage 2: per-tool multipliers (netherite vs iron etc) (toggleable)

## Folder layout (generated on first run)
plugins/BlockDurabilityTweaks/
  config.yml
  README-GENERATED.txt
  worlds/
    <world>/
      vanilla.yml
      profiles/
        vanilla.yml
        <your_profile>.yml

## Commands
/blockdurability info
/blockdurability reload
/blockdurability world list [world]
/blockdurability world load <world> <profile>
/blockdurability world save <world> <profile>
/blockdurability world reset <world>
/blockdurability world vanilla <world>

## Stage 2: per-tool multipliers
Enable:
- features.per_tool_multipliers: true
- tool_multipliers.enabled: true
Then set tool_multipliers.by_tool for the items you care about.
