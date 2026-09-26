# Riplow Client

Riplow is a Minecraft Bedrock/MCPE Android companion and launcher. It uses the Minecraft installation already on the device rather than bundling a second game.

## Current direction

- Clean Android launcher and module UI.
- Direct launch of the installed Minecraft Bedrock app.
- Expanded WClient-compatible module catalog with **WAura excluded**.
- Module cards show names and runtime state without per-module descriptions.
- Local Riplow modules can be enabled and configured.
- Game-side modules are bridge-gated until a verified Bedrock runtime adapter is connected.
- Bedrock-aware network diagnostics.
- No fake renderer or game-process access claims.

## Module groups

The catalog covers the existing Riplow performance, HUD, visual, network and Minecraft utilities plus the WClient module surface: combat, motion, visual, world and miscellaneous modules. Later WClient release features represented in the catalog include T P Mine, Block ESP, Stash Finder, Chest ESP, Fast Drop, Sus Chunk Finder, Config Manager, Shulker Preview, Old Motion Fly, Lifeboat Mode, Lifeboat Disabler, Phase and Ping Spoof.

The feature catalog is based on the public WClient project and its release history; Riplow does not copy WClient source code into this repository.

## Runtime boundary

The Android launcher can directly launch `com.mojang.minecraftpe`. Normal Android app sandboxing does not provide safe access to another app's private renderer or gameplay state. Riplow therefore distinguishes locally executable modules from modules that need a real Bedrock bridge instead of presenting a non-functional toggle as working.

## Native core

The C++20 layer provides native module state, Bedrock-oriented network diagnostics, an ARM64-safe pattern scanner, guarded page-protection helpers and a bounded render/touch pipeline. Process hooks are disabled in the normal companion build.

## Build

The project uses Kotlin/Android plus a small C++20 core. CI builds debug/release APKs, runs tests/lint, checks module registry consistency and verifies the resulting APKs.
