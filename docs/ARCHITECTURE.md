# Riplow Architecture

## Product model

Riplow is an external Minecraft Bedrock companion and launcher.

The app launches the user's installed Minecraft package directly. It does not bundle a fake Minecraft implementation, replace the game, inject a built-in ClickGUI, or keep a persistent overlay service running.

## Modules

The module registry is deliberately small and Minecraft-focused.

### Performance
FPS telemetry, frame pacing, performance profile, memory and battery.

### HUD
Session timer, clock, coordinates, compass, FPS, direction, potion, armor and scoreboard presentation.

### Visual
Zoom, crosshair, fullbright, fog, view bobbing, FOV, GUI scale and low-fire preferences.

### Network
Ping, jitter, packet loss, connection state and read-only Bedrock diagnostics.

### Minecraft
Pack profiles, add-on workspace, world organization, manual backups, version profiles, quick launch and configuration.

There are no combat modules or gameplay automation modules.

## Compatibility boundary

Riplow separates:
- app-side modules that can actually run in Android;
- native telemetry modules;
- Minecraft game modules that require a verified Bedrock bridge;
- planned Minecraft workspaces.

Unknown/new Minecraft versions do not silently receive unverified game hooks.

## Performance

The performance profile controls Riplow's own sampling and UI overhead. It does not claim to rewrite Mojang's renderer from an ordinary Android app sandbox.

## Network

Bedrock-aware diagnostics use the RakNet/UDP model and remain read-only. Riplow does not spoof or suppress server traffic.

## CI invariant

The Kotlin module registry and native C++ registry must contain the same number of definitions. Every capability set must reference a registered module.
