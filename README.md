# Riplow Client

Riplow is a Minecraft Bedrock/MCPE Android companion and launcher. It uses the Minecraft installation already on the device instead of pretending to be a second Minecraft app.

## Direction

- Clean landscape-first Android UI.
- Direct launch of the installed Minecraft Bedrock app.
- 36 Minecraft-focused modules.
- No Kill Aura, combat automation, auto-mining, auto-clicking, chest stealing, anti-AFK or similar gameplay automation.
- No persistent in-game overlay service or injected ClickGUI.
- Bedrock-aware network diagnostics.
- Explicit compatibility states: working, telemetry-only, bridge-required, or planned.
- Riplow-side performance profiles that reduce the companion's own overhead without fake renderer claims.

## Module groups

Performance covers FPS telemetry, frame pacing, performance profiles, memory and battery visibility.

HUD covers session timer, clock, coordinates, compass, FPS, direction, potion, armor and scoreboard presentation.

Visual covers zoom, crosshair, fullbright, fog, view bobbing, FOV, GUI scale and low-fire preferences.

Network covers ping, diagnostics, jitter, packet loss and connection state.

Minecraft covers pack workspaces, add-ons, world organization, backups, version profiles, quick launch and module configuration.

## Architecture boundary

Riplow launches the real com.mojang.minecraftpe installation. A normal Android app cannot safely pretend it has deep access to Minecraft's renderer or game state. When a module needs a verified Bedrock integration, Riplow marks it as bridge-required instead of exposing a fake working toggle.

## Build

The project uses Kotlin/Android plus a small C++20 core. CI checks that the Kotlin and native module registries stay synchronized.


## Native render foundation

The native layer includes an ARM64-safe pattern scanner, a page-protection guard, a thread-safe render pipeline, and a bounded non-blocking touch event queue.

The normal companion APK keeps the in-process renderer adapter disabled. A verified Bedrock integration build can opt into it with a separately supplied, pinned Dobby tree; the launcher itself does not claim access to another Android process's renderer.
