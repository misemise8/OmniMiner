# 1.21.1 All-Blocks Port Plan

## Summary

- Use the current `1.21.11-all-blocks` code as the base for a new `1.21.1-all-blocks` branch.
- Port the mod to Minecraft 1.21.1 / Fabric while preserving the existing all-blocks behavior.
- Save and verify this plan before changing implementation files.

## Key Changes

- Update Gradle and mod metadata for Minecraft 1.21.1:
  - `minecraft_version=1.21.1`
  - `yarn_mappings=1.21.1+build.3`
  - `fabric_version=0.116.12+1.21.1`
  - `cloth_config_version=15.0.140`
  - `modmenu_version=11.0.4`
- Keep public commands, config keys, and network payload IDs stable.
- Replace 1.21.11-specific highlight rendering APIs with 1.21.1-compatible rendering.
- Prefer Fabric world render events over a fragile `WorldRenderer.render` mixin where possible.
- Remove or disable the client render mixin if the event-based renderer replaces it.

## Test Plan

- Run `.\gradlew compileJava --console plain --rerun-tasks`.
- Run `.\gradlew build --console plain`.
- Check `git status --short --branch` and `git diff --stat`.
- If possible, run the client and manually verify keybind, preview highlight, HUD count, and config screen.

## Assumptions

- Branch name is `1.21.1-all-blocks`.
- The implementation should not commit or push unless explicitly requested.
- The plan file remains in the repository so it can be reviewed later.
