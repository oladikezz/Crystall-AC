<div align="center">

# 🛡️ Crystall AC

**Modern, Zero-Dataset Heuristic & Real-Time Statistical Anti-Cheat for Minecraft Servers**

[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![PaperMC](https://img.shields.io/badge/Paper-1.21.4%20%7C%2026.x-1C82AD?style=for-the-badge&logo=minecraft&logoColor=white)](https://papermc.io/)
[![ProtocolLib](https://img.shields.io/badge/ProtocolLib-5.3.0%2B-brightgreen?style=for-the-badge)](https://www.spigotmc.org/resources/protocollib.1997/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)
[![Build](https://img.shields.io/badge/Build-Passing-success?style=for-the-badge&logo=githubactions&logoColor=white)]()

<p align="center">
  <a href="#-key-features">Key Features</a> •
  <a href="#-architecture">Architecture</a> •
  <a href="#-detection-matrix">Detection Matrix</a> •
  <a href="#-installation">Installation</a> •
  <a href="#-commands--permissions">Commands</a> •
  <a href="#-building-from-source">Building</a>
</p>

---

</div>

## 🌟 Overview

**Crystall AC** is an enterprise-grade anti-cheat solution designed from the ground up for modern Paper and Spigot Minecraft servers. Unlike traditional solutions that rely on rigid static thresholds or fragile offline machine learning models, Crystall AC combines a **zero-dataset heuristic physics engine**, a **real-time unsupervised statistical baseline tracker (EWMA & Z-Score)**, and an **optional incremental SQLite learning layer** trained directly on confirmed moderator bans.

```
       [ Client Packets & Bukkit Events ]
                       │
             ┌─────────▼─────────┐
             │  VersionAdapter   │ ➔ (1.21.4 / 1.21.11 / 26.1.2 / 26.2)
             └─────────┬─────────┘
                       │
             ┌─────────▼─────────┐
             │  PlayerData Hub   │ ➔ Lag Compensation & Exemptions
             └────┬─────────┬────┘
                  │         │
 ┌────────────────▼───┐ ┌───▼───────────────────────────┐
 │   Heuristic Core   │ │ Real-Time Statistical Layer   │
 │ (Physics/Friction) │ │ (EWMA + Welford Z-Score O(1)) │
 └────────┬───────────┘ └───┬───────────────────────────┘
          └─────────┬───────┘
                    ▼
          ┌───────────────────┐
          │ Violation Manager │ ➔ Decay Scheduler (2s)
          └─────────┬─────────┘
                    ▼
          ┌───────────────────┐
          │ Punishment Ladder │ ➔ WARN ➔ KICK ➔ SHADOW BAN ➔ BAN
          └───────────────────┘
```

---

## ✨ Key Features

- ⚡ **Zero-Dataset Heuristic Core**: Functional immediately out-of-the-box with zero pre-training or offline data collection required.
- 📈 **Unsupervised Real-Time Baseline Tracker**:
  - Dynamically calculates individual player baselines using **EWMA** ($\alpha = 0.15$) and **Welford's Algorithm** for $O(1)$ sample variance.
  - Detects statistical anomalies via **Z-score** ($|Z| > 3.2$) without external dataset dependencies.
- 🔄 **Multi-Version Adapter Architecture**:
  - Strict decoupling of packet/physics logic via `VersionAdapter`.
  - Supports **1.21.4**, **1.21.11**, **26.1.2**, and **26.2**. Adding a new version only requires implementing a single adapter interface.
- 🗄️ **Incremental Retraining on Verified Bans (SQLite)**:
  - Buffers 5-minute rolling feature vectors for all players.
  - When staff executes `/ac ban <player> <reason>`, the snapshot is persisted to an embedded SQLite database.
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
| **Movement** | `Speed` | Vanilla kinematic friction modeling (air $0.91$, ground $0.6$, ice $1.45\times$, potion multipliers $+20\%/\text{lvl}$). |
| **Movement** | `Fly / NoFall` | Vertical acceleration limit ($\Delta Y \le 0.42 + \text{JumpBoost}$), air gravity verification ($\Delta Y_t = (\Delta Y_{t-1} - 0.08) \cdot 0.98$), and Ground-Spoof detection. |
| **Packet** | `Timer` | Microsecond-precision packet balance tracking against 50 ms server tick cadence. |

---

## 🚀 Installation

1. Download the latest `CrystallAC-1.0.0-RELEASE.jar` from [Releases](https://github.com/oladikezz/Crystall-AC/releases).
2. Ensure you have **ProtocolLib 5.3.0+** installed on your server.
3. Place `CrystallAC-1.0.0-RELEASE.jar` in your server's `plugins/` directory.
4. Start or restart your server.
5. Customize settings in `plugins/CrystallAC/config.yml`.

---

## 💻 Commands & Permissions

```
/ac <subcommand> [arguments]
```

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/ac check <player>` | `crystallac.admin` | View player ping, CPS, packet balance, and active VLs per check |
| `/ac stats <player>` | `crystallac.admin` | Display real-time statistical baseline (EWMA CPS, StdDev, rotation delta) |
| `/ac ban <player> <reason>` | `crystallac.admin` | Ban player and archive 5-minute feature snapshots to SQLite training dataset |
| `/ac reload` | `crystallac.admin` | Hot-reload configuration and recalculate check thresholds |
| `/ac alerts` | `crystallac.alerts` | Toggle in-game cheat alert notifications |

### Permissions

- `crystallac.admin` — Access to all administrator commands (Default: `op`)
- `crystallac.alerts` — Receive staff alert messages in chat (Default: `op`)
- `crystallac.bypass` — Bypass all anti-cheat checks (Default: `false`)

---

## ⚙️ Configuration (`config.yml`)

<details>
<summary><b>Click to expand sample config.yml</b></summary>

```yaml
settings:
  prefix: "&b&lCrystallAC &8» &f"
  debug: false
  log_to_file: true
  log_to_database: true
  lag_compensation:
    max_ping_threshold: 400
    min_server_tps: 18.0
    exemption_ticks_after_teleport: 20
    exemption_ticks_after_respawn: 40
    exemption_ticks_after_knockback: 15

checks:
  killaura:
    enabled: true
    max_vl: 100
    decay_amount: 1.0
    angle_snap_threshold: 38.5
    max_targets_per_tick: 1
    require_rotation_on_swing: true

  reach:
    enabled: true
    max_vl: 100
    decay_amount: 1.0
    base_max_reach: 3.05
    ping_compensation_multiplier: 0.0035
    hitbox_expansion: 0.1

  speed:
    enabled: true
    max_vl: 100
    decay_amount: 1.0
    base_walk_speed: 0.286
    base_sprint_speed: 0.362
    base_sprint_jump_speed: 0.612
    speed_potion_multiplier: 0.20
    ice_speed_multiplier: 1.45

  fly:
    enabled: true
    max_vl: 100
    decay_amount: 1.0
    max_upward_acceleration: 0.42
    verify_gravity: true
    ground_spoof_detection: true

  autoclicker:
    enabled: true
    max_vl: 100
    decay_amount: 0.8
    max_cps: 18
    min_deviation_ms: 4.8

statistical_layer:
  enabled: true
  ewma_alpha: 0.15
  zscore_threshold: 3.2
  anomaly_vl_multiplier: 1.5

punishments:
  decay_interval_seconds: 2
  tier_1_warn:
    vl_threshold: 10
    staff_alert: true
  tier_2_kick:
    vl_threshold: 25
    enabled: true
  tier_3_shadow_ban:
    vl_threshold: 45
    enabled: true
    duration_minutes: 10
  tier_4_ban:
    vl_threshold: 70
    enabled: true
    ban_command: "ban %player% [CrystallAC] Unfair Advantage: %check% (VL: %vl%)"
```

</details>

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