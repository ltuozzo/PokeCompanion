# PokeCompanion

An Android companion app for the [Ayn Thor](https://www.ayntec.com/products/thor) dual-screen device. Runs on the bottom screen while you play Pokemon games on the top screen — automatically detects which Pokemon you're fighting and displays its type weaknesses in real time.

## Features

- **Auto-detection** — screenshots the top screen every second, uses OCR to read the enemy Pokemon's name
- **1v1 and 2v2 support** — tabs to switch between both Pokemon in double battles
- **Full type chart** — weaknesses (×4, ×2), resistances (×0.5, ×0.25), and immunities shown by default
- **Gen 6+ and Gen 3 rulesets** — toggle between modern and GBA-era type rules per game profile
- **Works across all emulators and native game apps** — reads the screen directly, no emulator integration needed
- **Manual search** — look up any Pokemon outside of battle
- **Game profiles** — save a crop region and ruleset per game, switch between them instantly
- **Covers all generations** — Gen 1–3 bundled (386 Pokemon), Gen 4–9 addable

## Device

Designed for the **Ayn Thor** running Android 13. The app runs on the bottom display (display 1) and captures the top display (display 0) via the Android Accessibility Service — no root required.

## Project Status

| Session | Status | What was built |
|---|---|---|
| 1 — Data layer | ✅ Complete | Type enum, TypeChart (Gen 6+/Gen 3), WeaknessEngine, Room DB, pokemon.json |
| 2 — Screen capture | 🔜 Next | Accessibility Service, screenshot pipeline, hash check |
| 3 — OCR pipeline | ⏳ Pending | ML Kit OCR, name matching, 2v2 detection |
| 4 — Core UI | ⏳ Pending | Jetpack Compose, type badges, weakness card |
| 5 — Profiles | ⏳ Pending | Calibration flow, crop region, generation toggles |
| 6 — Polish | ⏳ Pending | Manual search, settings, edge cases |

## Setup

**Requirements:** Android Studio (or Android command-line tools) + a connected Ayn Thor with Developer Options enabled.

```bash
# 1. Generate Gradle wrapper (first time only — requires Gradle installed)
brew install gradle
bash scripts/setup.sh

# 2. Build and install on device
./gradlew installDebug

# 3. Run unit tests (no device needed)
./gradlew test
```

**Wi-Fi ADB** (optional, cable-free deploys after first setup):
```bash
# On Ayn Thor: Settings → Developer Options → Wireless Debugging → Pair device
adb pair <ip>:<port>   # one-time pairing
adb connect <ip>:<port>
# Then ./gradlew installDebug works over Wi-Fi
```

## Architecture

```
app/src/main/java/com/pokecompanion/
├── data/
│   ├── model/       — Type enum, PokemonEntity
│   ├── database/    — Room DAO, database, JSON populator
│   └── TypeChart.kt — 18×18 effectiveness matrix
├── engine/
│   └── WeaknessEngine.kt — compute(type1, type2?, gen3Rules) → WeaknessResult
└── MainActivity.kt  — entry point (UI added Session 4)
```
