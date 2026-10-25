# Requiem Forge Unofficial

An unofficial Forge port inspired by the [Requiem](https://github.com/Ladysnake/Requiem) mod for Minecraft 1.20.1.

> **Status: work in progress.** The port is functional but incomplete. Expect breaking changes.

## What it does

Requiem is a deep, immersive RPG layer for Minecraft. This port brings a subset of its mechanics to Forge:

- **Soul lifecycle** — souls can be bound, stored in vessels, released, and resurrected.
- **Possession** — players can possess most living entities (skeletons, zombies, foxes, villagers…), inheriting their passive behaviours while keeping their own inventory and hunger.
- **Rituals and runestones** — inert, rift, reclamation, attrition, emancipation, and penance runestones power soul manipulation and resurrection.
- **Vagrant fog / shadow player** — visual effects tied to restricted soul states.
- **Custom game rules** — `startingRemnantType`, `possessionDetection`, `possessionKeepInventory`.

## Installation

1. Install Minecraft 1.20.1 and Forge 47.4.16 or newer.
2. Drop `requiemunofficial-<version>.jar` into your `mods` folder.
3. (Optional) Install the runtime dependencies listed in `build.gradle` — Jade, Citadel, Alex's Mobs, Caelus, Origins — for full feature parity.

## Building from source

This project uses Gradle with the ForgeGradle plugin. Java 17 is required.

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

The output jar lands in `build/libs/`. To run locally:

```bash
# Client
gradlew runClient

# Server
gradlew runServer
```

## Configuration

Defaults live in `src/main/java/ua/meekeyn/requiemunofficial/common/config/RequiemConfig.java` (common) and `RequiemClientConfig.java` (client). They are exposed through Forge's mod config screen.

## Project layout

```
src/main/java/ua/meekeyn/requiemunofficial/   # ported mod sources
src/main/resources/assets/requiemunofficial/   # textures, models, shaders, lang
src/main/resources/data/requiemunofficial/     # loot tables, recipes, tags, structures
```

### Disabled content

Some recipes, loot tables, and advancements are disabled (`.disabled` extension) pending reimplementation. `build.gradle` excludes `**/*.disabled` from resource processing, so they are inert at build time.

## License

See [LICENSE](LICENSE). The mod is licensed under GPL-3.0-or-later. The original Requiem (Ladysnake) is licensed under GPL-2.0 — this port is an unofficial derivative and is not affiliated with the original project.

## Disclaimer

This is an **unofficial** port. "Minecraft" is a trademark of Mojang/Microsoft. All rights reserved. This mod is not endorsed by, associated with, or sponsored by Mojang or Microsoft.