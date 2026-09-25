# Riplow Architecture

## Principles

### UI is first-class
The launcher and in-game UI are the primary interaction layer. Touch targets, density, animation cost, accessibility, state persistence, and low-end Android performance are design requirements.

### Performance is measurable
No fake FPS-boost switches. Optimizations must be evaluated with frame time, FPS, 1% lows, memory, CPU/GPU utilization where available, and thermal behavior.

### Compatibility is explicit
Bedrock versions change. Integration must use capability detection and a compatibility layer rather than one permanent memory layout or offset set.

### Network is Bedrock-aware
Bedrock multiplayer networking uses RakNet over UDP. Riplow's network layer therefore focuses on latency, jitter, packet-loss diagnostics, connection stability, and safe client-side buffering rather than a TCP model.

## Initial module categories

- Performance
- PvP
- Utility
- Visual
- HUD
- Network
- Client

PvP features are limited to legitimate HUD, performance, accessibility, and diagnostics utilities; no gameplay automation or unfair-advantage systems.
