# PokeCompanion

An Android companion app for the [Ayn Thor](https://www.ayntec.com/products/thor) dual-screen device. Runs on the bottom screen while you play Pokemon games on the top screen — automatically detects which Pokemon you're fighting and displays its type weaknesses in real time.

## Features

- **Auto-detection** — screenshots the top screen every second, uses ML Kit OCR to read the enemy Pokemon's name
- **1v1 and 2v2 support** — tabs to switch between both Pokemon in double battles
- **Full type chart** — weaknesses (×4, ×2), resistances (×½, ×¼), and immunities with colour-coded type badges
- **Gen 6+ and Gen 3 rulesets** — toggle between modern and GBA-era type rules per profile
- **Manual search** — look up any Pokemon by name, filtered by the active profile's enabled generations
- **Game profiles** — save a crop region and ruleset per game; switch between them instantly from the top bar
- **Crop calibration** — capture the top screen and drag a box over the enemy name area to set the exact OCR region
- **Works across all emulators and native game apps** — reads the screen directly via Accessibility Service, no emulator integration needed
- **Offline-first** — all data and OCR run on-device; no internet required
- **Covers all generations** — Gen 1–3 bundled (386 Pokemon), Gen 4–9 addable

## Device

Designed for the **Ayn Thor** running Android 11+. The app runs on the bottom display and captures the top display via the Android Accessibility Service — no root required.

If the display IDs are reversed on your unit, swap them in Settings → Display ID.

## First-time Setup

**Requirements:** Gradle + a connected Ayn Thor with Developer Options enabled.

```bash
# 1. Install Gradle (first time only)
brew install gradle

# 2. Generate Gradle wrapper (first time only)
bash scripts/setup.sh

# 3. Build and install on device
./gradlew installDebug

# 4. On Ayn Thor: Settings → Accessibility → PokeCompanion → Enable
#    Grant POST_NOTIFICATIONS if prompted
```

**Wi-Fi ADB** (cable-free deploys after first pairing):
```bash
# On Ayn Thor: Settings → Developer Options → Wireless Debugging → Pair device
adb pair <ip>:<port>    # one-time pairing
adb connect <ip>:<port>
# ./gradlew installDebug now works over Wi-Fi
```

**Verify it's working:**
```bash
adb logcat -s PokeCompanion
# You should see:
#   DB + OCR pipeline ready
#   Service connected — polling display 0 every 1000ms
#   Detected: Geodude          ← during a battle
```

## Calibration

For best OCR accuracy, calibrate the crop region for each profile:

1. Open the app → **Profiles** → **Calibrate** next to a profile
2. Tap **Capture** — the app grabs a screenshot of the top screen
3. Drag a box over the area where enemy Pokemon names appear
4. Tap **Save**

Without a crop region the full screenshot is scanned, which works but may produce false positives from menu text.

## Architecture

```
app/src/main/java/com/pokecompanion/
├── data/
│   ├── model/          — Type enum, PokemonEntity, ProfileEntity
│   ├── database/       — Room DAOs, database, JSON populator
│   ├── profile/        — ProfileManager (active profile StateFlow, CRUD)
│   ├── settings/       — SettingsManager (display ID, poll interval, gen3 default)
│   └── TypeChart.kt    — 18×18 effectiveness matrix (Gen 6+ and Gen 3 variants)
├── detection/
│   ├── DetectionState.kt          — StateFlow bridge between service and UI
│   ├── DetectionResult.kt         — sealed class: None / Single / Double
│   ├── ImageHasher.kt             — aHash (8×8) + Hamming distance for change guard
│   ├── OcrPipeline.kt             — crop → ML Kit OCR → DB lookup → DetectionResult
│   └── PokeAccessibilityService.kt — 1s polling loop, screenshot, hash guard, OCR dispatch
├── engine/
│   └── WeaknessEngine.kt          — compute(type1, type2?, gen3Rules) → WeaknessResult
└── ui/
    ├── theme/TypeColors.kt         — official type colour palette
    ├── components/
    │   ├── TypeBadge.kt            — coloured badge with type name + multiplier
    │   └── WeaknessCard.kt         — Pokemon header + weakness/resistance rows
    └── screens/
        ├── MainScreen.kt           — main view, search mode, profile chip bar
        ├── ProfileScreen.kt        — profile list: create, delete, calibrate, gen3 toggle
        ├── CalibrationScreen.kt    — drag-to-crop on a live screenshot
        └── SettingsScreen.kt       — display ID, poll interval, gen3 default

app/src/main/assets/
└── pokemon.json    — 386 Pokemon, Gen 1–3, canonical Gen 9 types
```

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| Room | 2.6.1 | Local Pokemon + profile database |
| ML Kit Text Recognition | 16.0.1 | On-device OCR |
| Jetpack Compose BOM | 2024.05.00 | UI framework |
| Coroutines | 1.8.0 | Async DB and OCR operations |
| Gson | 2.10.1 | JSON → DB population on first run |
