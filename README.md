<div align="center">

# 🛡️ Crystall AC

**Modern, Zero-Dataset Heuristic & Real-Time Statistical Anti-Cheat for Minecraft Servers**

[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![PaperMC](https://img.shields.io/badge/Paper-1.21.4%20%7C%2026.x-1C82AD?style=for-the-badge&logo=minecraft&logoColor=white)](https://papermc.io/)
[![ProtocolLib](https://img.shields.io/badge/ProtocolLib-5.3.0%2B-brightgreen?style=for-the-badge)](https://www.spigotmc.org/resources/protocollib.1997/)
[![Discord](https://img.shields.io/badge/Discord-Webhooks-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)
[![Build](https://img.shields.io/badge/Build-Passing-success?style=for-the-badge&logo=githubactions&logoColor=white)]()

<p align="center">
  <a href="#-key-features">Key Features</a> •
  <a href="#-architecture">Architecture</a> •
  <a href="#-detection-matrix">Detection Matrix</a> •
  <a href="#-moderation--gui">Moderation & GUI</a> •
  <a href="#-installation">Installation</a> •
  <a href="#-commands--permissions">Commands</a> •
  <a href="#-building-from-source">Building</a>
</p>

---

</div>

## 🌟 Overview

**Crystall AC** is an enterprise-grade anti-cheat solution designed from the ground up for modern Paper and Spigot Minecraft servers. Unlike traditional solutions that rely on rigid static thresholds or fragile offline machine learning models, Crystall AC combines a **zero-dataset heuristic physics engine**, a **real-time unsupervised statistical baseline tracker (EWMA & Z-Score)**, an **active rubberband setback mitigation layer**, and an **optional incremental SQLite learning layer** trained directly on confirmed moderator bans.

```
       [ Client Packets & Bukkit Events ]
                       │
             ┌─────────▼─────────┐
             │  VersionAdapter   │ ➔ (1.21.4 / 1.21.11 / 26.1.2 / 26.2)
             └─────────┬─────────┘
                       │
             ┌─────────▼─────────┐
             │  PlayerData Hub   │ ➔ Lag Compensation, Exemptions & Valid Positions
             └────┬─────────┬────┘
                  │         │
 ┌────────────────▼───┐ ┌───▼───────────────────────────┐
 │   Heuristic Core   │ │ Real-Time Statistical Layer   │
 │ (Physics/Friction) │ │ (EWMA + Welford Z-Score O(1)) │
 └────────┬───────────┘ └───┬───────────────────────────┘
          └─────────┬───────┘
                    ▼
          ┌───────────────────┐
          │ Active Mitigation │ ➔ Instant Setback (Rubberband) & Attack Cancellation
          └─────────┬─────────┘
                    ▼
          ┌───────────────────┐
          │ Violation Manager │ ➔ Decay Scheduler (2s) & Discord Webhook Alerts
          └─────────┬─────────┘
                    ▼
          ┌───────────────────┐
          │ Punishment Ladder │ ➔ WARN ➔ KICK ➔ SHADOW BAN ➔ BAN
          └───────────────────┘
```

---

## ✨ Key Features

- ⚡ **Zero-Dataset Heuristic Core**: Functional immediately out-of-the-box with zero pre-training or offline data collection required.
- 🛑 **Active Setback & Combat Cancellation**:
  - Automatically rubberbands players back to their `lastValidLocation` when illegal `Speed` or `Fly` movement is flagged.
  - Cancels illegal combat damage on severe `Reach` and `Killaura` anomalies.
- 📈 **Unsupervised Real-Time Baseline Tracker**:
  - Dynamically calculates individual player baselines using **EWMA** ($\alpha = 0.15$) and **Welford's Algorithm** for $O(1)$ sample variance.
  - Detects statistical anomalies via **Z-score** ($|Z| > 3.2$) without external dataset dependencies.
- 🖥️ **Interactive In-Game Admin Dashboard (Chest GUI)**:
  - `/ac gui` — Server metrics, memory, online players, and one-click check toggles.
  - `/ac gui <player>` — Player inspection profile, live CPS/ping graphs, quick freeze, spectate, and ban buttons.
- ❄️ **Staff Moderation Tools (Freeze & Spectate HUD)**:
  - `/ac freeze <player>` — Locks suspected cheater in place with on-screen titles and chat audit prompts.
  - `/ac spectate <player>` — Silent spectate mode with real-time Action Bar HUD displaying Target CPS, Reach, Ping, and VL.
- 📢 **Discord Webhook Alerts**:
  - Asynchronous rich Discord Embeds containing player avatar, check name, VL, ping, TPS, coordinates, and timestamp.
- 🔄 **Multi-Version Adapter Architecture**:
  - Strict decoupling of packet/physics logic via `VersionAdapter`.
  - Supports **1.21.4**, **1.21.11**, **26.1.2**, and **26.2**.
- 🗄️ **Incremental Retraining on Verified Bans (SQLite)**:
  - Buffers 5-minute rolling feature vectors for all players.
  - When staff executes `/ac ban <player> <reason>`, the snapshot is persisted to an embedded SQLite database (`data.db`).
  - Automatically fine-tunes heuristic sensitivity once $\ge 25$ confirmed samples are reached.
- 🔒 **4-Tier Punishment Ladder & Shadow Ban**:
  - **Tier 1 (WARN)**: Staff alerts with detailed violation breakdown.
  - **Tier 2 (KICK)**: Automated disconnection.
  - **Tier 3 (SHADOW BAN)**: Quarantines cheaters silently (packet hiding from tab/world & cancels outgoing damage).
  - **Tier 4 (BAN)**: Executes server ban commands.

---

## 🎯 Detection Matrix

| Category | Check | Detection Principle |
| :--- | :--- | :--- |
| **Combat** | `Killaura` | Multi-target in single tick, angle snaps ($> 38.5^\circ$), hits outside FOV ($> 105^\circ$), attacks without prior swing. |
| **Combat** | `Reach` | Dynamic raycasting from eye origin to target AABB bounding box with ping latency compensation: $\text{Reach}_{\text{max}} = 3.05 + (\text{Ping}_A + \text{Ping}_B) \cdot 0.0035 + 0.10\text{b}$. |
| **Combat** | `Autoclicker` | CPS hard limit ($> 18$) and click interval standard deviation consistency test ($\sigma < 4.8\text{ ms}$ at $\text{CPS} \ge 13$). |
| **Combat** | `Aimbot` | Mouse sensitivity quantization analysis (GCD divisor check) and pitch-axis locking during fast yaw sweeps. |
| **Movement** | `Speed` | Vanilla kinematic friction modeling (air $0.91$, ground $0.6$, ice $1.45\times$, potion multipliers $+20\%/\text{lvl}$) with automatic setback. |
| **Movement** | `Fly / NoFall` | Vertical acceleration limit ($\Delta Y \le 0.42 + \text{JumpBoost}$), air gravity verification ($\Delta Y_t = (\Delta Y_{t-1} - 0.08) \cdot 0.98$), and Ground-Spoof detection. |
| **World** | `Scaffold` | Placing blocks beneath feet while sprinting backwards or off-angle without sneaking. |
| **World** | `FastPlace` | Placement delays $< 45\text{ ms}$ or multi-block placement in a single tick. |
| **World** | `FastBreak` | Breaking blocks faster than tool physics permit or breaking through solid barriers without line of sight. |
| **Packet** | `Timer` | Microsecond-precision packet balance tracking against 50 ms server tick cadence. |
| **Packet** | `BadPackets` | Pitch angles $> 90^\circ$, NaN/Infinity coordinate payloads, and packet flood exploits. |
| **Inventory** | `InventoryMove` | Sprinting / jumping at full speed with open container inventories (AutoTotem / ChestStealer). |

---

## 🎮 Moderation & GUI

### Admin Dashboard (`/ac gui`)
Run `/ac gui` to open the central administrative control panel:
- **Server Health**: Real-time TPS, heap memory, player count.
- **Checks Manager**: Click any check icon to enable or disable it on-the-fly without server restarts.
- **Player Inspector**: Audit online players, inspect VL breakdowns, reset VL, or execute instant moderation actions.

### Player Freeze (`/ac freeze <player>`)
Temporarily locks the suspect:
- Cancels movement, block interactions, inventory clicks, and damage.
- Sends visual Title/Subtitle screen prompts and chat instructions.

### Spectator Mode with HUD (`/ac spectate <player>`)
Puts the administrator in silent spectator mode with an **Action Bar HUD** updated 4 times per second:
```
[CrystallAC HUD] Target: Steve | CPS: 14.2 | Ping: 42ms | RotΔ: 18.5° | VL: 24.0
```

---

## 🚀 Installation

1. Download the latest `CrystallAC-1.0.0-RELEASE.jar` from [Releases](https://github.com/oladikezz/Crystall-AC/releases).
2. Ensure you have **ProtocolLib 5.3.0+** installed on your server (optional but recommended).
3. Place `CrystallAC-1.0.0-RELEASE.jar` in your server's `plugins/` directory.
4. Start or restart your server.
5. Customize settings and Discord webhook URL in `plugins/CrystallAC/config.yml`.

---

## 💻 Commands & Permissions

```
/ac <subcommand> [arguments]
```

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/ac gui [player]` | `crystallac.gui` | Open interactive Admin Dashboard or inspect target player |
| `/ac check <player>` | `crystallac.admin` | View player ping, CPS, packet balance, and active VLs per check |
| `/ac stats <player>` | `crystallac.admin` | Display real-time statistical baseline (EWMA CPS, StdDev, rotation delta) |
| `/ac freeze <player>` | `crystallac.freeze` | Freeze player in place for screen-share or audit |
| `/ac unfreeze <player>` | `crystallac.freeze` | Unfreeze player |
| `/ac spectate <player\|stop>` | `crystallac.spectate` | Enter silent spectator mode with real-time HUD |
| `/ac ban <player> <reason>` | `crystallac.admin` | Ban player and archive 5-minute feature snapshots to SQLite training dataset |
| `/ac reload` | `crystallac.admin` | Hot-reload configuration and recalculate check thresholds |
| `/ac alerts` | `crystallac.alerts` | Toggle in-game cheat alert notifications |

---

## 🔨 Building from Source

### Prerequisites
- **JDK 21** or higher
- **Maven 3.8+**

```bash
# Clone the repository
git clone https://github.com/oladikezz/Crystall-AC.git
cd Crystall-AC

# Build the shaded JAR
mvn clean package
```

The compiled binary will be generated at:
`target/CrystallAC-1.0.0-RELEASE.jar`

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).