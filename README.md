# CraftAttack Spawn Elytra

[![Build and Test](https://github.com/MarkapToGo/craftattack-spawn-elytra/actions/workflows/build.yml/badge.svg)](https://github.com/MarkapToGo/craftattack-spawn-elytra/actions/workflows/build.yml)
[![Paper Version](https://img.shields.io/badge/Paper-26.2-blue.svg)](https://papermc.io)
[![Java Version](https://img.shields.io/badge/Java-25-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](#license)

A modern, high-performance **Paper 26.2** plugin bringing the iconic **CraftAttack spawn elytra boost** mechanic to your Minecraft server.

Players within the configured world spawn radius can double-jump to glide with a temporary elytra—no chestplate required. While gliding, they can launch themselves forward in their look direction with a configurable velocity boost.

---

## Features

- **Spawn Elytra Gliding**: Double-jump in Survival or Adventure mode inside the world spawn radius to deploy a temporary elytra glide.
- **Directional Look Boost**: Trigger a velocity boost aimed precisely where the player is looking.
- **Dual Activation Modes**:
  - **`SWAP`**: Trigger boost using the offhand swap key (<kbd>F</kbd> by default).
  - **`SNEAK`**: Trigger boost on sneak transition (<kbd>Shift</kbd> by default).
- **Damage & Landing Protection**: Complete immunity to fall damage and kinetic wall impact damage while flying, plus a 1-second landing grace period.
- **Adventure & MiniMessage Localization**: Full rich-text support in `language.yml` with colors, gradients, and hex styling.
- **Dynamic Client Keybind Resolution**: Displays the player's personal keybind via `<key>` (e.g. shows the player's custom swap or sneak key in the action bar).
- **Optimized Performance**:
  - Distance-squared calculations (`distanceSquared`) avoiding expensive square root math.
  - Safe UUID-based tracking preventing memory leaks on disconnects, respawns, teleports, or world switches.
  - Non-blocking scheduler task with graceful shutdown cleanup.
- **Hot-Reload Support**: `/spawnelytra reload` reloads configuration and language files without server restarts.
- **Backwards Compatible**: Seamlessly supports both modern namespaced config keys and legacy flat keys.

---

## Supported Versions

| Component | Supported Version |
| :--- | :--- |
| **Server Platform** | **Paper 26.2** (or compatible forks) |
| **Java Runtime** | **Java 25** (LTS) |
| **API Version** | `26.2` |

---

## Installation

1. Download the latest `craftattackspawnelytra-1.2.1.jar` release from the releases page or [build it from source](#building-from-source).
2. Place the `.jar` file into your server's `plugins/` directory.
3. Start or restart your server to generate the default configuration files:
   - `plugins/CraftAttackSpawnBoost/config.yml`
   - `plugins/CraftAttackSpawnBoost/language.yml`
4. Customize the settings in `config.yml` and translations in `language.yml`.
5. Execute `/spawnelytra reload` in-game or from the server console to apply your changes.

---

## Activation Modes

CraftAttack Spawn Elytra supports two configurable activation modes via `activation.mode` in `config.yml`:

### 1. `SWAP` (Default)
- **Keybind**: Player's offhand swap key (Default: <kbd>F</kbd> / `key.swapOffhand`).
- **Behavior**: When airborne with spawn elytra, pressing the swap key triggers the boost. The item swap event is cancelled so items are **not** swapped between the player's main hand and offhand.

### 2. `SNEAK`
- **Keybind**: Player's sneak key (Default: <kbd>Shift</kbd> / `key.sneak`).
- **Behavior**: When airborne with spawn elytra, initiating a sneak triggers the boost.
- **Edge-Triggered**: The boost triggers cleanly on sneak **initiation** (`PlayerToggleSneakEvent.isSneaking() == true`). Holding down the sneak key will **not** cause repeated activations or velocity glitches.

> [!NOTE]
> Each flight allows exactly one boost. Once boosted, players cannot boost again until they land and initiate a new double-jump glide.

---

## Configuration (`config.yml`)

The configuration file is located at `plugins/CraftAttackSpawnBoost/config.yml`.

### Example `config.yml`

```yaml
# ===================================================
# CraftAttack Spawn Elytra Configuration
# ===================================================

activation:
  # Available modes:
  # SWAP  - Activate boost using the player's offhand swap key (default: F)
  # SNEAK - Activate boost when the player starts sneaking (Shift)
  mode: SWAP

elytra:
  # Radius in blocks around the world spawn where Elytra gliding is enabled
  spawn-radius: 50

  # Velocity multiplier applied when boosting
  multiplier: 5.0

  # Whether the boost feature is enabled
  boost-enabled: true

  # World name where the spawn elytra is active
  world: "world"
```

### Settings Breakdown

| Setting | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `activation.mode` | `String` | `SWAP` | Activation mode: `SWAP` (offhand swap key) or `SNEAK` (sneak key transition). |
| `elytra.spawn-radius` | `Integer` | `50` | Radius in blocks centered at the world spawn where double-jump glide is permitted. |
| `elytra.multiplier` | `Double` | `5.0` | Velocity vector multiplier applied in the player's look direction upon boost. |
| `elytra.boost-enabled` | `Boolean` | `true` | When `false`, players can still glide from spawn, but the boost action is disabled. |
| `elytra.world` | `String` | `"world"` | Name of the world where spawn elytra gliding is active. Dynamically resolved if loaded late. |

### Legacy Key Compatibility

Existing configurations using legacy flat keys are automatically recognized and supported:

- `spawnRadius` $\rightarrow$ `elytra.spawn-radius`
- `multiplyValue` $\rightarrow$ `elytra.multiplier`
- `boostEnabled` $\rightarrow$ `elytra.boost-enabled`
- `world` $\rightarrow$ `elytra.world`
- `message` $\rightarrow$ Action bar boost message fallback

---

## Language & Localization (`language.yml`)

All user-facing messages are localized via `plugins/CraftAttackSpawnBoost/language.yml` and rendered using **Adventure MiniMessage**.

### Example `language.yml`

```yaml
# ===================================================
# CraftAttack Spawn Elytra - Language Configuration
# ===================================================
# Supports Adventure MiniMessage formatting (<gray>, <yellow>, <bold>, <gradient>, etc.)
# Documentation: https://docs.advntr.dev/minimessage/format.html
#
# Available placeholders:
#   <prefix>         - Inserts the prefix defined below
#   <key>            - Inserts the keybind component based on the active boost mode (swap / sneak)
#   <key:identifier> - Inserts an explicit Minecraft keybind component (e.g. <key:key.swapOffhand>)

# Prefix prepended to command feedback and announcements
prefix: "<gray>[<aqua>SpawnElytra<gray>] "

messages:
  # Action bar message displayed when a player double-jumps to activate spawn glide
  boost-actionbar: "<gray>Drücke <yellow><key></yellow> um dich zu boosten."

  # Sent to the command sender when configuration and language files are successfully reloaded
  reload-success: "<prefix><green>Konfiguration und Sprache wurden erfolgreich neu geladen."

  # Sent when a player tries to execute an administrative command without permission
  no-permission: "<prefix><red>Dazu hast du keine Berechtigung."

  # Sent when invalid command arguments are supplied
  command-usage: "<prefix><yellow>Verwendung: <white>/spawnelytra reload"
```

### MiniMessage Formatting & Placeholders

- **Rich Text & Styling**: Use tags like `<yellow>`, `<aqua>`, `<bold>`, `<italic>`, `<color:#ff5555>`, or `<gradient:red:blue>`. See the [MiniMessage Documentation](https://docs.advntr.dev/minimessage/format.html).
- **`<prefix>`**: Dynamically inserts the parsed `prefix` component into messages.
- **`<key>`**: Dynamically resolves to the client-side keybind component corresponding to the active `activation.mode`:
  - Mode `SWAP` $\rightarrow$ Resolves to client keybind `key.swapOffhand`
  - Mode `SNEAK` $\rightarrow$ Resolves to client keybind `key.sneak`
  - Rendered by Minecraft in the client's selected language and custom key bindings (e.g., if a player remapped swap to <kbd>G</kbd>, they will see <kbd>G</kbd>).
- **`<key:identifier>`**: Allows inserting explicit vanilla keybind identifiers (such as `<key:key.jump>` or `<key:key.use>`).

---

## Commands & Permissions

### Commands

| Command | Aliases | Description |
| :--- | :--- | :--- |
| `/spawnelytra reload` | `/spawnboost reload`<br>`/craftattackspawnboost reload` | Reloads `config.yml` and `language.yml` from disk. |

Tab completion automatically suggests `reload` for players with admin permissions.

### Permissions

| Permission | Description | Default |
| :--- | :--- | :--- |
| `craftattack.spawnelytra.admin` | Allows reloading plugin configurations and language files. | `op` |
| `spawnelytra.admin` | Alias for `craftattack.spawnelytra.admin`. | `op` |

---

## Building from Source

### Prerequisites

- **Java Development Kit (JDK) 25** or higher
- Git

### Build Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/MarkapToGo/craftattack-spawn-elytra.git
   cd craftattack-spawn-elytra
   ```

2. Run the Gradle build:
   - **Linux / macOS**:
     ```bash
     ./gradlew build
     ```
   - **Windows**:
     ```cmd
     gradlew.bat build
     ```

3. Run the automated test suite:
   ```bash
   ./gradlew test
   ```

4. The compiled JAR artifact will be located at:
   ```text
   build/libs/craftattackspawnelytra-1.2.1.jar
   ```

---

## Continuous Integration (CI)

This project uses **GitHub Actions** for automated building and testing on every push and pull request to `main`.

- **Workflow**: `.github/workflows/build.yml`
- **Runner**: `ubuntu-latest`
- **JDK Distribution**: Eclipse Temurin Java 25
- **Steps**: Checks out code, configures Gradle, executes JUnit 5 tests, compiles the JAR, and publishes the build artifact.

Status Badge:
```markdown
[![Build and Test](https://github.com/MarkapToGo/craftattack-spawn-elytra/actions/workflows/build.yml/badge.svg)](https://github.com/MarkapToGo/craftattack-spawn-elytra/actions/workflows/build.yml)
```

---

## Project Architecture

```text
src/main/java/de/coolepizza/craftattack/
├── ActivationMode.java           # Enum defining SWAP and SNEAK trigger styles and Adventure keybind mapping
├── CraftAttackSpawnBoost.java    # Main JavaPlugin entry point, lifecycle management & reload orchestrator
├── command/
│   └── SpawnElytraCommand.java   # /spawnelytra reload handler with permissions and tab-completion
├── config/
│   └── PluginConfig.java         # Type-safe configuration loader with validation and legacy fallbacks
├── listener/
│   └── SpawnBoostListener.java   # Event handler, flight tracker, landing detector & tick scheduler
└── message/
    └── MessageService.java       # MiniMessage parsing, language.yml manager & dynamic keybind resolver
```

### Key Architectural Highlights

1. **State Cleanliness & Memory Safety**: Players are tracked by `UUID` rather than direct `Player` references. Flight state and protection timestamps are safely purged on quit, world change, teleportation, respawn, or gamemode switch.
2. **Landing Detection**: `SpawnBoostListener` checks `player.isOnGround()`, `isInWater()`, `isInLava()`, and solid block collisions with a 300ms takeoff guard to avoid premature flight cancellation.
3. **Fall & Wall Impact Protection**: Players flying via the plugin or in their 1-second post-landing grace period are immune to `FALL` and `FLY_INTO_WALL` damage.
4. **Dynamic World Resolution**: If the configured world is not yet loaded when the plugin initializes, it resolves dynamically as soon as the world becomes available.

---

## Credits & Attribution

- Original plugin and video concept by **cedricmkl**:
  - YouTube: [Spawn Elytra Video](https://www.youtube.com/watch?v=S9f_mFiYT50)
  - SpigotMC: [Spawn Elytra Resource #97565](https://www.spigotmc.org/resources/spawnelytra.97565/)
- Modernized for Paper 26.2 and Java 25 with Adventure MiniMessage support by **MarkapToGo**.

---

## License

This project is licensed under the [MIT License](LICENSE).