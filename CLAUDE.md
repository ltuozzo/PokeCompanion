# PokeCompanion — Claude Briefing

Android companion app for the Ayn Thor dual-screen device. Runs on the bottom screen while Pokemon games play on the top screen. Detects which Pokemon the user is fighting via screenshot OCR and displays type weaknesses.

## Architecture Decisions

| Decision | Choice | Why |
|---|---|---|
| Screen capture | Accessibility Service `takeScreenshot(displayId)` | Rootless, works across all apps |
| OCR | ML Kit Text Recognition | On-device, fast, no internet needed |
| Detection trigger | 1s polling + perceptual hash guard | Balanced battery/responsiveness |
| Idle state | Keep last detected Pokemon | No annoying clear on overworld |
| Pokemon data | Local Room DB (all gens) | Offline-first |
| Type rules | Gen 6+ default, Gen 3 variant available | Configurable per profile |
| UI framework | Jetpack Compose (added Session 4) | Native Android, modern |
| Min SDK | 30 (Android 11) | Required for `takeScreenshot(displayId)` |

## Display IDs

The Ayn Thor top screen is assumed to be display `0`. Verify during first test deployment.
A "Swap displays" toggle will be in Settings if display IDs are reversed.

## Session Progress

### Session 1 — COMPLETE
- ✅ Project scaffold (Gradle KTS, KSP, Room, AppCompat)
- ✅ `Type.kt` — 18-type enum, `gen3Types` list, `fromString()`
- ✅ `TypeChart.kt` — full Gen 6+ effectiveness matrix + Gen 3 variant (no Fairy; Steel resists Ghost/Dark)
- ✅ `WeaknessEngine.kt` — `compute(type1, type2?, gen3Rules)` → `WeaknessResult`
- ✅ `WeaknessResult` — quadWeak, weak, halfResist, quarterResist, immune
- ✅ `PokemonEntity.kt`, `PokemonDao.kt`, `PokeDatabase.kt`, `DatabasePopulator.kt`
- ✅ `pokemon.json` — Gen 1–3 (386 Pokemon, canonical Gen 9 types)
- ✅ Unit tests: `WeaknessEngineTest`, `TypeChartTest`

**Data note:** `pokemon.json` uses current (Gen 9) canonical types. Pokemon that changed type in Gen 6
(Clefairy→Fairy, Ralts→Psychic/Fairy, Mawile→Steel/Fairy, etc.) are stored with their current types.
The `gen3Rules` flag in `WeaknessEngine.compute()` controls the type chart rules, not the Pokemon's stored types.
Gen 4–9 data (~650 more Pokemon) can be added later from PokeAPI before Session 3.

### Session 2 — NEXT
- Accessibility Service class + AndroidManifest permission
- `takeScreenshot(displayId = 0)` call
- Perceptual hash comparison (pHash of cropped bitmap region)
- 1-second polling loop (foreground service with notification)
- Verify correct display ID on physical device

### Session 3
- ML Kit Text Recognition dependency
- Hardcoded crop region (configurable in Session 5)
- OCR result → Pokemon name lookup in DB using enabled-generation filter
- Multi-block detection for 2v2 (2 name matches = 2v2)

### Session 4
- Jetpack Compose dependencies + ComposeActivity setup
- Weakness card UI with official-style type badges (icon + text + multiplier label)
- 2v2 tab layout (PagerState with 2 tabs)
- Auto ON/OFF toggle (bottom-left)
- Search button stub (bottom-right)

### Session 5
- Profile data model (crop region + ruleset + enabled generations)
- Calibration flow: screenshot → user drags box → save region
- Profile CRUD + last-used profile persistence
- Profile switcher in top bar

### Session 6
- Manual search UI (compact list above keyboard, filtered by enabled gens)
- Search auto-disables Auto mode
- Settings screen: display ID swap, default ruleset, polling interval
- Edge case polish

## Key File Paths

```
app/src/main/java/com/pokecompanion/
├── data/
│   ├── model/
│   │   ├── Type.kt              — 18-type enum
│   │   └── PokemonEntity.kt     — Room entity (id, name, type1, type2, generation)
│   ├── database/
│   │   ├── PokemonDao.kt        — findByName, search (filtered by generations), insertAll
│   │   ├── PokeDatabase.kt      — singleton Room DB
│   │   └── DatabasePopulator.kt — first-run JSON → DB import
│   └── TypeChart.kt             — effectiveness map, gen3 lazy variant
├── engine/
│   └── WeaknessEngine.kt        — compute() + WeaknessResult data class
└── MainActivity.kt              — stub (UI in Session 4)

app/src/main/assets/
└── pokemon.json                 — 386 Pokemon, Gen 1–3

app/src/test/java/com/pokecompanion/
├── data/TypeChartTest.kt
└── engine/WeaknessEngineTest.kt
```

## Build & Deploy

```bash
# First-time setup — run once (requires Gradle installed)
brew install gradle
bash scripts/setup.sh          # generates gradlew + gradle-wrapper.jar

# Build and push to connected device
./gradlew installDebug

# Run unit tests (no device needed)
./gradlew test

# Filter app logs on device
adb logcat -s PokeCompanion

# Enable Wi-Fi ADB (do once over USB, then cable-free forever)
# On Ayn Thor: Settings → Developer Options → Wireless Debugging → Pair
# On Mac: adb pair <ip>:<port>  →  adb connect <ip>:<port>
```

## Dependencies (app/build.gradle.kts)

| Library | Version | Purpose |
|---|---|---|
| Room runtime + KTX | 2.6.1 | Local Pokemon database |
| Room compiler (KSP) | 2.6.1 | Annotation processing |
| KSP | 1.9.24-1.0.20 | Fast annotation processor |
| Gson | 2.10.1 | JSON → DB population |
| Coroutines Android | 1.8.0 | Room async operations |
| AppCompat | 1.7.0 | Activity base class |
| ML Kit Text Recognition | — | Added Session 3 |
| Jetpack Compose BOM | — | Added Session 4 |
