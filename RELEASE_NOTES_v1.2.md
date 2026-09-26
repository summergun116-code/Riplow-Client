# Riplow Client v1.2

## Minecraft / Bedrock compatibility
- Added Bedrock 2026 version normalization for both `26.51` and `1.26.51` style version strings.
- Added a compatibility catalog for stable Bedrock releases and protocol metadata.
- Added conservative future-version handling so unknown/new Bedrock versions remain in generic-safe mode instead of enabling unverified native hooks.
- Added a read-only RakNet/UDP server probe with MOTD, player count, protocol, version, latency, and protocol-mismatch diagnostics.
- Added Minecraft server deep-link handoff using the official `minecraft://connect` style flow.
- Added regression coverage for the 1.26.51 / protocol 2193 server advertisement.

## Performance / FPS
- Added an adaptive Riplow Performance Policy with Balanced, Performance, and Extreme modes.
- FPS Boost Profile now controls real Riplow overlay/diagnostic polling overhead instead of being a display-only toggle.
- Reduced background HUD refresh frequency in higher-performance modes.
- Made the native frame-metrics path allocation-free during diagnostics to avoid avoidable heap churn.
- Improved 1% low calculation for small sample windows.
- Added frame-sample sanity filtering to ignore invalid/extreme samples.
- Kept performance instrumentation measurable rather than claiming renderer changes that are not available through Android's normal app sandbox.

## Client / ClickGUI
- Added the complete 100-feature master catalog with explicit implementation states.
- Added searchable module navigation.
- Added module categories, settings metadata, profiles, configuration import/export, and bridge-state gating.
- Prevented planned/game-bridge modules from being presented as working toggles.

## Lifeboat
- Added a dedicated conservative Lifeboat compatibility policy.
- Added candidate detection and explicit session confirmation before the profile becomes active.
- Added an allowlist-driven safety model that disables unsupported/prohibited features while the profile is active.
- Riplow does not intercept, spoof, suppress, or modify server anti-cheat/kick traffic.
- Added regression tests for Lifeboat profile activation and feature blocking.

## Reliability
- Hardened overlay teardown and panel animation cleanup against WindowManager races.
- Hardened server-probe executor shutdown/cancellation behavior.
- Hardened draggable overlay updates.
- Kept JNI failures isolated so the Android UI can still open if native loading fails.
- Added/updated regression tests across compatibility, server probing, master feature registration, and performance policy.

## Release scope
v1.2 is the consolidated compatibility, performance-runtime, UI, networking-diagnostics, and reliability release. Native Bedrock game hooks remain version-gated until a tested adapter proves compatibility; unknown versions never get unsafe native integration automatically.
