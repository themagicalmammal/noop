# Unwired Kotlin Twins — Android Parity Tracking

These files are faithful Kotlin ports of Swift modules that ARE wired into the iOS/macOS app.
They were restored for rewiring (commit `a2c72c8a0`) but have not yet been connected to the
Android UI or data layer. Each one compiles, has tests, and passes parity oracles — they just
have no caller in the Android app yet.

## Status Legend

| Status | Meaning |
|--------|---------|
| ⚪ Unwired | File exists, compiles, tested — no Android caller yet |
| 🟡 Partially wired | Some constants/types referenced, but core logic not invoked |
| 🟢 Wired | Fully integrated (listed here for completeness) |

---

## ⚪ Fully Unwired

### 1. `HevyApi.kt` — Hevy workout import parser
- **Path:** `android/app/src/main/java/com/noop/ingest/HevyApi.kt`
- **Swift twin:** `Packages/StrandImport/Sources/StrandImport/HevyApiParser.swift`
- **Purpose:** Parses Hevy API v1 JSON documents into WhoopStore value types. Pure/network-free —
  maps dictionaries to domain values. The iOS/macOS app uses `HevyAPIClient` for I/O and
  `HevyApiParser` for the mapping. On Android, the I/O layer and the screen that triggers a
  Hevy sync are not yet built.
- **Why it matters:** Hevy is a popular strength-training app. Users who track lifts in Hevy
  want their data in NOOP without manual entry.
- **To wire:** Build `HevyAPIClient` (network I/O), add a Data Sources row for Hevy in
  `ForgeSettingsHubs.kt`, and call `HevyApi.parse()` on fetched documents.
- **Risk:** Low — pure parsing, no BLE, no I/O in the file itself.

### 2. `AdaptiveExpenditureEngine.kt` — Long-horizon energy balance
- **Path:** `android/app/src/main/java/com/noop/analytics/AdaptiveExpenditureEngine.kt`
- **Swift twin:** `Packages/StrandAnalytics/Sources/StrandAnalytics/AdaptiveExpenditureEngine.swift`
- **Purpose:** Estimates daily energy expenditure adaptively from intake and weight series.
  Uses a rolling model with confidence levels (building → moderate → high). The iOS app
  shows this on the Energy/Biomarkers screen.
- **Why it matters:** Gives users a personalized TDEE estimate that improves over time,
  rather than a static BMR formula.
- **To wire:** Feed it from `Repository`'s weight and calorie series, display on the
  Body/Energy page (`ForgeBody.kt` or `ForgeFood.kt`).
- **Risk:** Low — pure math, database-free.

### 3. `WorkoutTypeClassifier.kt` — Activity type classifier
- **Path:** `android/app/src/main/java/com/noop/analytics/WorkoutTypeClassifier.kt`
- **Swift twin:** `Packages/StrandAnalytics/Sources/StrandAnalytics/WorkoutTypeClassifier.swift`
- **Purpose:** Coarse classifier (walk/run/strength/cycle/ski/other) from already-captured
  signals: HR profile, activity-class ticks, gravity variance, duration/calories. No raw IMU.
  Advisory only — labels detected workouts, never gates anything.
- **Why it matters:** Automatically labels detected workouts so users don't have to tag them.
  The iOS app uses this on the workouts screen.
- **To wire:** Run it against `WorkoutDetector` outputs in `AnalyticsEngine.kt` or on the
  workouts screen, display the label as a suggestion.
- **Risk:** Low — advisory instrumentation, no destructive path.

### 4. `PreSleepHeartRateFeedback.kt` — Pre-sleep HR reading
- **Path:** `android/app/src/main/java/com/noop/analytics/PreSleepHeartRateFeedback.kt`
- **Swift twin:** `Packages/StrandAnalytics/Sources/StrandAnalytics/PreSleepHeartRateFeedback.swift`
- **Purpose:** Opt-in descriptive reading: compares HR in the declared pre-sleep window
  against a personal rolling baseline. Delivered the following morning. No diagnosis,
  no action recommendation.
- **Why it matters:** Gives users a nightly HR fact without alarmist framing. The iOS app
  shows this as a card on the Today screen.
- **To wire:** Add a card in `ForgeToday.kt`, read the HR timeline from `Repository`,
  gate behind an opt-in toggle in settings.
- **Risk:** Low — descriptive only, shares `MetricCfg` baseline config.
- **Note:** Uses `SleepHeartRateContrast.VALID_MIN_BPM` / `VALID_MAX_BPM` constants (see below).

### 5. `SleepHeartRateContrast.kt` — Sleep vs wake HR contrast
- **Path:** `android/app/src/main/java/com/noop/analytics/SleepHeartRateContrast.kt`
- **Swift twin:** `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepHeartRateContrast.swift`
- **Purpose:** Descriptive comparison of HR during primary-sleep vs wake windows.
  Deliberately emits NO dipper/non-dipper labels, NO risk categories — wellness context only.
- **Why it matters:** Answers "how does my sleep HR compare to my waking HR?" without
  diagnostic framing. iOS shows this on the Sleep/Health screens.
- **To wire:** Add to the Sleep tab (`ForgeSleep.kt`) or Health tab as a descriptive card.
  Caller supplies the two windows; this file just computes the contrast.
- **Risk:** Low — descriptive math, no thresholds that gate anything.
- **Note:** Referenced by `PreSleepHeartRateFeedback.kt` for its constants.

### 6. `OuraRingEpochScan.kt` — Oura ring time-anchor forensics
- **Path:** `android/app/src/main/java/com/noop/oura/OuraRingEpochScan.kt`
- **Swift twin:** `Packages/WhoopStore/Sources/WhoopStore/OuraRingEpochScan.swift`
- **Purpose:** Groups Oura sidecar rows by the ring epoch each one implies, to detect
  sessions filed under a wrong time anchor (the ticks×10 defect, #2239/#2252). Purely
  descriptive — identifies constant time shifts without classifying restart vs mis-anchor.
- **Why it matters:** Oura Ring 5 users on Android can't rely on the iOS-side epoch scan.
  This gives them the same diagnostic tool.
- **To wire:** Add to the Test Centre or a Data Sources diagnostic page. Reads decoded
  sidecars from the store, no BLE needed.
- **Risk:** Low — read-only store query, no writes.

### 7. `PmdControl.kt` — Polar PMD control point commands
- **Path:** `android/app/src/main/java/com/noop/polar/PmdControl.kt`
- **Swift twin:** `Packages/PolarProtocol/Sources/PolarProtocol/PmdControl.swift`
- **Purpose:** Builds PMD Control Point command bytes (start/stop measurement, read
  settings) for Polar heart-rate straps. The DATA side (`PmdDecoder`) is already wired;
  this is the CONTROL side — the commands that tell the strap to start/stop sending.
- **Why it matters:** Without this, Android can't start a Polar strap's measurement
  stream. The decoder works but has nothing to decode until control is wired.
- **To wire:** Add a `PolarPMDSource` BLE wrapper in `com.noop.ble`, discover the PMD
  service, write control bytes via `WhoopBleClient`, subscribe to data notifications.
- **Risk:** Medium — hardware-gated, BLE writes. The response layout is per SDK but not
  yet confirmed against a real device (same caveat as the Swift twin).

### 8. `BatteryGuidedCapture.kt` — Battery test mode flow
- **Path:** `android/app/src/main/java/com/noop/testcentre/BatteryGuidedCapture.kt`
- **Swift twin:** `Strand/System/BatteryGuidedCapture.swift`
- **Purpose:** The guided "wear it N days, then Report" flow for Battery test mode (#713).
  Arms the daily scheduled export so a redacted bundle drops each morning while the mode
  runs. Days-twin of the Sleep nights flow.
- **Why it matters:** Makes the Battery test mode usable — without it, the user has to
  manually trigger exports. iOS has this flow.
- **To wire:** Add a Battery row to Test Centre (`TestCentreScreen.kt`), call `arm()` /
  `disarm()` on mode start/end, show progress with `BatteryGuidedCaptureProgress`.
- **Risk:** Low — reuses `ScheduledDebugExport`, no new scheduler.

---

## 🟡 Partially Wired

### 9. `PreSleepHeartRateFeedback.kt` → `SleepHeartRateContrast.kt`
- `SleepHeartRateContrast` constants are referenced by `PreSleepHeartRateFeedback`, but
  neither has an Android UI caller. When `PreSleepHeartRateFeedback` gets wired,
  `SleepHeartRateContrast` comes along.

---

## 🟢 Wired (for reference)

### `LiftMetrics.kt` — Strength session metrics
- **Path:** `android/app/src/main/java/com/noop/analytics/LiftMetrics.kt`
- **Swift twin:** `Packages/StrandAnalytics/Sources/StrandAnalytics/LiftMetrics.swift`
- **Status:** Wired into `ForgeTrainingSessionDetail.kt` — shows muscle counts and
  session load on the session detail page. Parity-tested against the Swift oracle.

---

## Permanently Removed

### `EcgResearchAllowList.kt` — ECG panel opcode safety guard
- **Why removed:** No Swift twin (Android-only file). The actual BLE send path was no
  longer consulting it (unwired). Its safety role (allow-list of ECG opcodes, deny-list
  of firmware-load opcodes) was either moved elsewhere or the ECG research panel was
  changed upstream. Removed in commit `bc5bb14fb`.
- **If the ECG panel returns:** The allow-list pattern should be restored — opcode 139
  sits just below the firmware-load family (142-144).

### `ui-tooling-preview` Gradle dependency
- **Why removed:** Unused in `build.gradle.kts`. No preview annotations referenced it.

---

## What "wiring" means for each

| File | Needs | Effort |
|------|-------|--------|
| `HevyApi.kt` | Network client + settings row + trigger button | Medium |
| `AdaptiveExpenditureEngine.kt` | Data feed from Repository + UI card | Low |
| `WorkoutTypeClassifier.kt` | Integration with workout detection + label display | Low |
| `PreSleepHeartRateFeedback.kt` | HR timeline feed + Today card + opt-in toggle | Medium |
| `SleepHeartRateContrast.kt` | Window selection logic + UI card | Low |
| `OuraRingEpochScan.kt` | Test Centre or diagnostic page | Low |
| `PmdControl.kt` | BLE source wrapper + service discovery | Medium-High |
| `BatteryGuidedCapture.kt` | Test Centre row + progress UI | Low |

---

## Parity contract reminder

Per AGENTS.md: analytics and stored data must be **byte-identical** across Swift and Kotlin.
Each unwired file is pinned by a parity oracle test (or should be) — the expected values
in the Kotlin test are the Swift oracle's stdout. When wiring these, run:

```bash
# Verify the oracle still matches after any change:
cd android && ./gradlew testFullDebugUnitTest --tests "com.noop.*Parity*" --tests "com.noop.*Oracle*"
```

Don't let the numbers drift because the UI is different. SwiftUI Charts vs Compose Canvas
is fine; the values must not be.
