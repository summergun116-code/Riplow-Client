# Riplow Client

Riplow is a Minecraft Bedrock/MCPE Android client and launcher architecture focused on performance, clean UI, utility features, diagnostics, and maintainable native integration.

> Performance is the blood of Riplow.

## Goals

- Mobile-first glass UI with clean spacing and subtle motion.
- C++20 native core with a thin Android/JNI layer.
- Modular client architecture with categories, settings, lifecycle, and version requirements.
- Performance telemetry: FPS, frame time, 1% lows, CPU/GPU/RAM where available, thermals, and network quality.
- Bedrock-aware version compatibility instead of hard-coded offsets.
- Legitimate managed profiles/import/launch workflows that respect Android sandboxing and Minecraft platform rules.
- Network diagnostics built around Bedrock's RakNet/UDP model.
- Reproducible builds and automated checks.

## Development order

1. Plan and architecture
2. UI foundation
3. Native core
4. Minecraft integration boundary
5. Modules and HUD
6. Performance instrumentation
7. Compatibility
8. Testing and benchmarking
9. Release APK

Riplow uses other Bedrock clients and launchers as engineering references where useful, while keeping its own implementation and UI.
