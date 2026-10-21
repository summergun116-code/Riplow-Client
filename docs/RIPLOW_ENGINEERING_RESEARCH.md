# Riplow Engineering Research: FPS, Frame Generation, Replay Studio, and Glass UI

**Status:** Research/architecture proposal  
**Scope:** Existing Riplow Client repository; Android companion/launcher for Minecraft Bedrock/MCPE  
**Goal:** Build real, measurable features with clear capability boundaries instead of cosmetic toggles.

## Executive decision

Build the next version in this order:

1. **Make performance telemetry truthful and useful.**
2. **Ship a safe Device Performance Advisor and a Bedrock Performance Pack workflow.**
3. **Polish the landscape UI into a restrained black/white glass interface.**
4. **Add a user-consented screen-recording and Replay Studio utility.**
5. Keep *live frame generation* and *world-reconstructing 3D replay* behind an explicit future game-renderer/game-state integration requirement.

Riplow currently launches the user's installed Minecraft app. Under normal Android app sandboxing, Riplow cannot directly read or patch Minecraft's private memory, change its renderer, or feed new frames into the game's swap chain. Don't label a device monitor as "Minecraft FPS", don't claim a profile boosts the game unless it demonstrably changes an available game setting, and don't ship nonfunctional "Frame Gen" or "Replay Mod" switches.

## Repository audit (main branch, reviewed 2026-10-21)

Relevant files:
- `app/src/main/AndroidManifest.xml`: Riplow is landscape and queries `com.mojang.minecraftpe`; a foreground UDP relay service is declared.
- `app/build.gradle.kts`: Kotlin/Android app, minSdk 26, targetSdk 35, C++20 via CMake.
- `app/src/main/cpp/core/performance.cpp`: rolling 240-sample frame-time buffer, average FPS, and a tail-based low-FPS metric.
- `app/src/main/cpp/core/RenderPipeline.cpp`: optional EGL swap hook; tracks hook-observed frame counts/timestamps, but the reviewed callback does not feed frame durations into `PerformanceEngine::record_frame`.
- `app/src/main/cpp/CMakeLists.txt`: `RIPLOW_ENABLE_PROCESS_HOOKS` is off by default and needs a separately provided Dobby tree to enable.
- `app/src/main/java/com/riplow/client/modules/ModuleCapabilities.kt`: features are classified as ready, native telemetry, game-bridge-required, or planned.
- `app/src/main/java/com/riplow/client/modules/ModuleRuntime.kt`: the current performance profile returns the selected profile label; it does not itself tune Minecraft.
- `app/src/main/java/com/riplow/client/MainActivity.kt` and `activity_main.xml`: current UI uses classic Android Views/XML.

**Immediate bug/architecture finding:** the C++ metric engine can compute numbers, but it needs a real, valid frame-time source. The default companion build deliberately does not install process hooks. This means the safe first release should expose *device telemetry* and honest *game integration status* separately. Never fabricate Minecraft FPS from launcher UI refreshes or device refresh rate.

## 1. Architecture: separate responsibilities

### Kotlin/Android owns
- UI, navigation, glass surfaces, animations and accessibility.
- Device facts: display modes, battery, thermal status, memory available to Android, app installation/version and user-selected files.
- Scoped Storage / Storage Access Framework imports and exports for user-selected packs, add-ons, profiles, backups, and recordings.
- MediaProjection consent and lifecycle; recording service, clip library, bookmarks and export.
- Persistent settings, capability display, profile recommendations and session lifecycle.

### C++20 owns only work that benefits from it
- Allocation-bounded numeric telemetry and reusable statistics.
- Optional low-level code only behind well-defined interfaces and tests.
- No duplicated UI state or duplicate feature catalog.
- No process hooks in the normal companion build.
- No pretend boost functions that only change a setting label.

JNI calls should be coarse-grained and bounded; do not poll the native bridge every frame from the Kotlin main thread. The UI should consume immutable snapshots updated on a background executor.

### Keep a capability contract
Every module should have:
- Stable ID and category.
- A capability: `READY_IN_RIPLOW`, `DEVICE_TELEMETRY`, `USER_ACTION_REQUIRED`, `GAME_BRIDGE_REQUIRED`, or `UNAVAILABLE`.
- A real handler/service if it is toggleable.
- A clear disabled reason if it cannot run.
- Unit tests for its action and persistence.

The module list currently exists in both Kotlin and C++. Reduce registry drift by making Kotlin the catalog owner and passing only required IDs/settings into native code, or generate both registries from one checked-in machine-readable definition. Never enable a module simply because it appears in the catalog.

## 2. FPS optimizer: what we can actually ship

### A. Device telemetry (first)
Create a `DevicePerformanceRepository` and immutable `PerformanceSnapshot` containing:
- Supported display modes and maximum refresh rate.
- Current device thermal status (when supported).
- Battery percentage and temperature when available.
- System available RAM and Riplow's own process memory, shown separately.
- Storage headroom for recordings/packs.
- Session duration and sampling timestamp.
- Actual Minecraft telemetry only if a future adapter supplies it, with source and confidence displayed.

On Android 10+ (API 29+) use `PowerManager` thermal-status callbacks where available; use safe fallbacks on minSdk 26 devices. Do not infer that high free RAM means Minecraft itself is using little RAM. Refresh rate is a display capability, not proof that Minecraft is rendering at that FPS.

### B. Safe Performance Advisor
Treat “Balanced / Performance / Sustained” as policies, not magic CPU overclock buttons:
- **Balanced:** normal telemetry cadence, moderate visual effects in Riplow, battery-aware advice.
- **Performance:** show a checklist of user-applied Minecraft settings and pack recommendations; do not imply system clocks are unlocked.
- **Sustained:** watch thermal state and advise a lower sustainable target when the device heats up, reducing recording load first.
- **Battery Saver:** prefer lower display refresh targets only where the user/system can actually apply them.

The companion can recommend Minecraft's in-game graphics, render distance, simulation distance, particle, and visual-effects settings according to the installed version. It generally cannot set these private game options from another app. The UI must mark these as “Recommended” or “Open Minecraft settings”, not “Applied”.

### C. Bedrock Performance Pack / pack analyzer
Build a user-controlled workflow:
1. Import an `.mcpack` or resource-pack folder using the Android file picker.
2. Analyze the files the user selected: package size, texture dimensions, largest assets, duplicate-looking files, malformed manifests and incompatible format versions.
3. Offer an optional **Riplow Performance** resource pack with low/medium texture subpacks where supported.
4. Explain the visual trade-off; back up Riplow-managed profiles before replacing them.
5. Let the user open Minecraft to activate the imported pack manually when necessary.

The analyzer must not claim every texture-size reduction raises FPS. It can reduce memory pressure/loading cost on texture-heavy packs; the gain varies with GPU, texture cache, scenes and Minecraft version. Don't modify private game files or assume access to `Android/data`.

Microsoft's current creator guidance recommends managing texture size/resource usage and using subpacks for device tiers. Their guidance also notes that 1024×1024 textures consume sixteen times the uncompressed texture memory of 256×256 textures. Use the official documentation in the references below to inform limits.

### D. Measurements before claims
Benchmark repeatably:
- Use the same device, Minecraft version, resource pack, world/location, camera direction and graphics settings.
- Record a baseline, apply one change, repeat, then compare.
- Report median/p95/p99 frame time and thermal state only when an actual trustworthy game frame-time source exists.
- If no game adapter is connected, use “device status” metrics and “Minecraft FPS unavailable in companion mode”.
- Don't promise percentage gains without measurements on named test devices.

## 3. Frame generation: name and scope it honestly

A live frame-generation system usually needs access to render frames at the right point in the graphics pipeline and, for high-quality interpolation, motion vectors/depth or equivalent scene information. It must present interpolated images through the game's renderer/driver with careful latency management. A companion APK has no general Android public API that lets it inject generated frames into a separate Minecraft application's swap chain.

**What to build now:** a real **Frame Stability** page, not a fake frame generator:
- Display supported refresh modes and thermal status.
- Show frame-time telemetry when a trustworthy source is attached.
- Recommend sensible targets (30/45/60/90/120 only when the device and game can use them).
- Explain frame pacing versus rendered FPS.
- Show “Live frame generation requires a verified game-renderer adapter” until one exists.

Android's Frame Pacing / Swappy library is intended to be integrated by the game renderer using OpenGL ES or Vulkan. The Android Frame Rate API sets the desired rate for an app's own surface and the system is free to ignore a request. Calling it on Riplow's window does not set Minecraft's window. Game Mode interventions are device/OS-controlled and are not a generic third-party “boost another app” API.

If a renderer integration ever becomes possible, treat frame generation as a separate research project with:
- Supported Minecraft versions/render backend matrix.
- A design for motion data, interpolation, frame presentation and input latency.
- Thermal/memory budgets and an on/off safety gate.
- Image-quality tests for particles, UI, fast camera turns, transparency and HUD.
- Clear no-op/fallback behavior if the adapter is missing.
Do not build a misleading checkbox now.

## 4. Replay Studio: useful now, true ReplayMod later

ReplayMod's official documentation describes a Minecraft **Java Edition** mod that requires Fabric on modern versions (or Forge on older versions); this is not a drop-in Bedrock Android companion feature.

### Stage 1: Clip Studio (feasible)
- User taps Record; Android's MediaProjection consent flow appears.
- Capture the selected app/game window on supported Android versions, or the whole display where required.
- Encode MP4 via MediaCodec/MediaMuxer; use a foreground service, notification and explicit Stop control.
- Provide bookmarks/timestamps, clip list, trim/rename/delete, share/export, and recording quality presets.
- Monitor heat, battery and storage; warn that recording uses GPU/memory/encoder resources and may reduce game performance.
- Request microphone/audio capture separately and clearly, if supported.

### Stage 2: Instant Replay (after Stage 1 is stable)
Keep short segmented clips in a rolling buffer (for example, 5-second segments) and preserve the last 15–30 seconds when the user taps Save. Implement segment cleanup, timestamp stitching, key-frame handling, process-death recovery and free-space limits. Test on real phones before promising this feature; continuous encoding has an ongoing battery/FPS cost. Start with conventional recording if this work is not stable.

### Stage 3: World-aware replay (future integration)
A true 3D replay with free camera, time scrubbing, player/entity reconstruction and hide/show controls needs synchronized Bedrock world/entity/camera state capture from a working supported integration. Screen video can be replayed, but it cannot become an editable 3D scene on its own. Do not describe Clip Studio as a full ReplayMod clone.

## 5. Clean glass UI (without making the app slower)

Keep the current landscape orientation. Use a calm monochrome glass language:
- Near-black base, charcoal panels, white primary text and gray secondary text.
- Semi-transparent panels with a faint one-pixel border and subtle gradient; 14–20 dp rounded corners.
- Use backdrop blur sparingly or not at all on scrolling cards; low-opacity surfaces can create the glass feel without continuously expensive blur.
- Large readable headings, compact module rows, consistent spacing and generous touch targets.
- Category navigation on the left; home/dashboard content on the right; concise status chips for Ready, Telemetry, Needs game bridge and Planned.
- Performance page: device health, thermal/battery strip, profile recommendations and charts only when data exists.
- Replay page: large record/stop control, recent clips, timeline/bookmarks and quality/storage status.
- Use short fade/slide transitions; honor the reduced-motion setting.
- Keep module rows free of verbose descriptions; show the module name, state and settings affordance.
- Use reusable `GlassCard`, `StatusChip`, `MetricTile`, `SectionHeader` and `ModuleRow` styles rather than hand-styling every screen differently.

Because the existing app is XML/Views-based, first build reusable shape drawables/styles and small custom view helpers. Move to Jetpack Compose only if its benefits outweigh migration cost; do not rewrite everything just to get rounded transparent cards. Avoid continuous animated blur and 60 Hz chart redraws. Update passive telemetry at a modest interval (for example, once per second) and charts only when new samples arrive.

## 6. Suggested file layout

```text
app/src/main/java/com/riplow/client/
  performance/
    DevicePerformanceRepository.kt
    PerformanceSnapshot.kt
    ThermalPolicy.kt
    PerformanceAdvisor.kt
    GameTelemetrySource.kt
    PackAnalyzer.kt
  recording/
    ReplayCaptureService.kt
    ProjectionSession.kt
    ClipStore.kt
    ClipMetadata.kt
    ClipMerger.kt              # only after segmented recording is tested
  ui/
    theme/GlassStyles.kt       # or XML styles/drawables first
    components/GlassCard.kt
    components/StatusChip.kt
    components/MetricTile.kt
app/src/main/cpp/core/
  performance.cpp/.hpp         # bounded stats, only with real samples
  performance_stats.cpp        # percentile/percentile-low tests
docs/
  RIPLOW_ENGINEERING_RESEARCH.md
```

## 7. Implementation order / acceptance checks

### Phase 1 — truthful performance baseline
- Add thermal/battery/display/memory snapshot APIs with API-level fallbacks.
- Separate device metrics from Minecraft metrics in types and labels.
- Fix metrics definitions and add tests for empty data, invalid frame times, ring wrap, percentile/low-FPS math.
- Make the profile selector return a recommendation/configuration and accurately list what has and has not been applied.
- No FPS claims without valid source samples.

### Phase 2 — working performance pack tools
- Import through Storage Access Framework.
- Validate resource-pack manifest and texture dimensions.
- Provide low/medium texture pack variants only with explicit opt-in and visible visual trade-offs.
- Test import/export and rollback with current supported Minecraft installs.

### Phase 3 — UI refactor
- Build the reusable landscape glass design system.
- Make capability/status states consistent and accessible.
- Remove inert toggles; every Ready item needs an action test.
- Keep effects cheap on low/mid-range devices.

### Phase 4 — Replay Studio
- Start with consent-based screen recording and clip library.
- Test cancellation, rotation/window selection, app switch, screen lock, low storage, thermal warnings and service shutdown.
- Add segmented instant replay only after video and audio tracks remux correctly on tested devices.

### Phase 5 — advanced renderer integration
- Re-evaluate only if a legal, versioned and reliable Bedrock integration provides render or world state. Keep the companion build safe and fully usable if there is no adapter.

CI should test Kotlin capability mapping, native statistics, import validation, settings persistence and recording lifecycle helpers. For each release, test the installed APK on real devices, not only on GitHub Actions.

## References

- Android Frame Pacing / Swappy: https://developer.android.com/games/sdk/frame-pacing
- Android Game Mode API and interventions: https://developer.android.com/games/optimize/adpf/gamemode/gamemode-api
- Android refresh-rate guidance: https://developer.android.com/games/optimize/display-refresh-rate-change
- Android Dynamic Performance Framework: https://developer.android.com/games/optimize/adpf
- Android Thermal API: https://developer.android.com/games/optimize/adpf/thermal
- Android MediaProjection capture and consent: https://developer.android.com/media/grow/media-projection
- Minecraft Bedrock performance/resource use: https://learn.microsoft.com/en-us/minecraft/creator/documents/practices/improvingperformanceandresourceusage
- Minecraft Bedrock resource packs: https://learn.microsoft.com/en-us/minecraft/creator/documents/resourcepack
- ReplayMod documentation (Java/Fabric/Forge): https://www.replaymod.com/docs/
- Android NDK and CMake: https://developer.android.com/ndk/guides/cmake
