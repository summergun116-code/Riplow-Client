# Riplow Client v1.3

## Major redesign

v1.3 changes Riplow from a generic ClickGUI-heavy client into a focused Minecraft companion and launcher.

### Minecraft stays Minecraft
- Riplow launches the installed Bedrock package directly.
- Removed the persistent overlay service and foreground-service notification path.
- Removed the Android overlay permission from the normal app flow.
- The app no longer depends on an injected in-game menu for its main navigation.

### Focused module surface
- Reduced the module registry to 36 Minecraft-focused modules.
- Removed combat, mining automation, chest stealing, anti-AFK, auto-reconnect and other gameplay automation concepts.
- Modules now expose honest availability states instead of fake active toggles.

### UI
- Reworked the app into a clean launcher/dashboard with separate Home, Modules, Packs, Performance, Network and Settings sections.
- Removed the old in-game ClickGUI architecture from the primary flow.
- Module cards use compact status states and only show live toggles for app-side modules.

### Performance
- Lowered architectural overhead by removing the always-running overlay service.
- Kept local performance profiles for Riplow's own polling, diagnostics and animation work.
- Avoided renderer/FPS claims that cannot be performed from a normal Android app sandbox.

### Reliability
- Minecraft launch is a direct package handoff.
- Diagnostics are refresh-on-demand in the app.
- CI keeps Kotlin and native module definitions synchronized.
