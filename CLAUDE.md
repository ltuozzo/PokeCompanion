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

### Session 2 — COMPLETE
- ✅ `detection/PokeAccessibilityService.kt` — 1s polling loop, `takeScreenshot(displayId)`, hash comparison, logs on change
- ✅ `detection/ImageHasher.kt` — aHash (8×8 average hash), `distance()`, `isSame(threshold=5)`
- ✅ `detection/NotificationHelper.kt` — persistent foreground notification, IMPORTANCE_LOW channel
- ✅ `res/xml/accessibility_service_config.xml` — `canTakeScreenshot="true"`, no event types needed
- ✅ `AndroidManifest.xml` — service declaration, `BIND_ACCESSIBILITY_SERVICE`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`
- ✅ `ImageHasherTest.kt` — Hamming distance + isSame threshold tests

**Display ID note:** `PokeAccessibilityService.displayId` defaults to `0`. Change to `1` if the bottom screen is captured instead. A Settings toggle (Session 6) will expose this.

**To test Session 2:**
1. Build and install: `./gradlew installDebug`
2. On Ayn Thor: Settings → Accessibility → PokeCompanion → Enable
3. Grant POST_NOTIFICATIONS if prompted
4. Run: `adb logcat -s PokeCompanion`
5. Open a Pokemon game on the top screen — you should see "Screen changed" log lines when the display updates

### Session 3 — COMPLETE
- ✅ `mlkit-text-recognition` 16.0.1 added to version catalog + `app/build.gradle.kts`
- ✅ `detection/DetectionResult.kt` — sealed class: `None`, `Single(pokemon)`, `Double(pokemon1, pokemon2)`
- ✅ `detection/OcrPipeline.kt` — crops bitmap → ML Kit OCR → iterates text blocks → DB lookup → DetectionResult
- ✅ `detection/PokeAccessibilityService.kt` — wired to OCR; initialises DB + pipeline async; `lastResult` held on `None`
- ✅ 2v2 support: 2 DB hits from OCR text blocks → `DetectionResult.Double`
- ✅ `defaultCropRect = null` (full screenshot) for initial testing; Session 5 adds calibration UI
- ✅ `enabledGenerations` companion var (all gens by default); Session 5 replaces with per-profile value

**OCR tuning notes:**
- `cropRect = null` during early testing is intentional — calibrate region in Session 5
- ML Kit returns multiple `TextBlock`s per image; we iterate all and stop at 2 hits
- `findByName` uses `LOWER(name) = LOWER(:name)` — exact match required; OCR must produce clean names
- If OCR accuracy is low on GBA fonts, a fuzzy prefix search via `search()` can be tried as fallback

### Session 4 — COMPLETE
- ✅ Compose BOM 2024.05.00, activity-compose 1.9.0, lifecycle-runtime-compose 2.7.0
- ✅ `detection/DetectionState.kt` — singleton StateFlow bridge (result + isAutoEnabled)
- ✅ `ui/theme/TypeColors.kt` — official type colours + readable content colour per type
- ✅ `ui/components/TypeBadge.kt` — coloured badge with type name + multiplier label
- ✅ `ui/components/WeaknessCard.kt` — header (name + own types) + FlowRow weakness rows
- ✅ `ui/screens/MainScreen.kt` — dark bg, 2v2 TabRow, Auto ON/OFF toggle, Search stub
- ✅ `MainActivity.kt` — swapped to ComponentActivity + setContent + darkColorScheme
- ✅ `PokeAccessibilityService` — posts to DetectionState; skips OCR when Auto OFF

### Session 5 — COMPLETE
- ✅ `data/model/ProfileEntity.kt` — Room entity: name, crop rect (4 nullable ints), gen3Rules, enabledGenerations, isLastUsed
- ✅ `data/database/ProfileDao.kt` — getAll, getLastUsed, insert, update, delete, clearLastUsed, markLastUsed
- ✅ `data/database/PokeDatabase.kt` — version 2, MIGRATION_1_2 (adds profiles table), profileDao()
- ✅ `data/profile/ProfileManager.kt` — singleton: init(context), activeProfile StateFlow, create/delete/switchTo/saveCrop
- ✅ `detection/DetectionState.kt` — added pendingCalibration + calibrationBitmap StateFlows
- ✅ `detection/PokeAccessibilityService.kt` — observes active profile → updates OcrPipeline; posts calibration screenshot
- ✅ `ui/screens/CalibrationScreen.kt` — shows top-screen screenshot; drag-to-draw crop rect; saves to profile
- ✅ `ui/screens/ProfileScreen.kt` — profile list with Select / Calibrate / Delete; create dialog
- ✅ `ui/screens/MainScreen.kt` — profile chip row (scrollable), Profiles button; AppContent() handles navigation
- ✅ `MainActivity.kt` — calls ProfileManager.init() so profiles load without the service

**Calibration coordinate mapping note:**
Screen drag coords → bitmap coords uses: `scale = min(canvasW/bmpW, canvasH/bmpH)`;
then `bmpX = (screenX - imgLeft) / scale`, clamped to [0, bmpW].

### Session 6 — COMPLETE
- ✅ `data/settings/SettingsManager.kt` — SharedPreferences wrapper: displayId, pollIntervalMs, defaultGen3Rules
- ✅ `ui/screens/SettingsScreen.kt` — display ID swap, polling interval, default gen3 toggle, Accessibility Settings shortcut
- ✅ `ui/screens/MainScreen.kt` — search mode (debounced, filtered by active profile gens), service-not-enabled banner, ⚙ Settings button
- ✅ `ui/screens/ProfileScreen.kt` — gen3Rules toggle per profile
- ✅ `ui/components/WeaknessCard.kt` — gen3Rules parameter wired through; `remember(pokemon, gen3Rules)` key
- ✅ `data/profile/ProfileManager.kt` — search(query) suspend fun + updateGen3Rules()
- ✅ `detection/PokeAccessibilityService.kt` — poll delay reads SettingsManager.pollIntervalMs
- ✅ `MainActivity.kt` — SettingsManager.init() called on launch

**Search UX:**
- Tapping "Search" → Auto OFF, search bar opens in content area
- Tapping a result → WeaknessCard shown, "Search" button becomes "Close"
- Auto ON toggle → clears manual selection and collapses search

**All 6 sessions complete. App is fully functional.**

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
├── detection/
│   ├── DetectionResult.kt       — sealed class: None / Single / Double
│   ├── ImageHasher.kt           — aHash, distance, isSame
│   ├── NotificationHelper.kt    — foreground notification
│   ├── OcrPipeline.kt           — crop → ML Kit → DB lookup → DetectionResult
│   └── PokeAccessibilityService.kt — 1s polling, hash guard, OCR dispatch
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
| ML Kit Text Recognition | 16.0.1 | On-device OCR for Pokemon name detection |
| Jetpack Compose BOM | — | Added Session 4 |
