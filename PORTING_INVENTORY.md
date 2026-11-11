# Porting Inventory — Aura 12.1.1 → noop-minimal

Baseline: branch `aura` @ `ee0d76ef` (v12.1.0, build 439) vs Aura 12.1.1 reference (v12.1.1, build 1239).
Reference (read-only): `/home/dipannanda/Documents/Repo/Aura-12.1.1-source`.
Platform tags: **iOS** = `NOOPiOS` target, **macOS** = `Strand` target, **Android** = `android/` app (native reimplementation required by the parity contract).

## Summary

- **New files in Aura: 565** (all missing from repo; zero already present)
- **Modified files: 648** — direction: aura-lead: 49, both-substantial (true merge): 276, repo-lead (keep repo, verify no lost Aura feature): 552, minor: 46 (categories overlap per file)
- **Deleted files: 0** — 581 repo-only files/dirs (android/, NOOPWatch*, docs/, .github/, Tools/, StrandTests/, Packages Tests, generated project) are kept untouched

### Feature areas (new files)

1. **iOS resources (icons, Rive, plist)** — 68 new files
2. **Analytics math** — 59 new files
3. **AI coach "Svea"** — 50 new files
4. **Fitness UI** — 43 new files
5. **Scale (BLE body scale)** — 40 new files
6. **Acts staged screens** — 40 new files
7. **iOS UI kit (NoopUI)** — 29 new files
8. **Screens (shared iOS+macOS)** — 25 new files
9. **Design system (tokens, charts, icons)** — 23 new files
10. **Training engine (workouts/programs/strength)** — 22 new files
11. **Data layer (Repository)** — 21 new files
12. **Fitness engine (cardio, HR zones, importers)** — 14 new files
13. **Goals UI** — 14 new files
14. **Biscuit companion pet** — 11 new files
15. **Storage (WhoopStore/GRDB)** — 9 new files
16. **AI providers (Svea)** — 9 new files
17. **System services (notifications, updates, background)** — 9 new files
18. **Widget panels** — 9 new files
19. **Journal/logging UI** — 8 new files
20. **Aura share cards** — 7 new files
21. **Importers (Hevy, Strava, Mi, Google, Health)** — 6 new files
22. **iOS shared (widgets/live-activity attrs)** — 5 new files
23. **App resources (assets, plist, entitlements)** — 4 new files
24. **HealthKit (iOS)** — 4 new files
25. **iOS system (intents, sync, streaks)** — 4 new files
26. **Live Activities (iOS)** — 4 new files
27. **On-device LLM (llama.cpp + Gemma)** — 4 new files
28. **WHOOP protocol** — 3 new files
29. **AI background tasks (iOS)** — 3 new files
30. **Companion (Biscuit/Svea) UI** — 3 new files
31. **Widgets / Live Activities (iOS)** — 3 new files
32. **App shell / root views** — 2 new files
33. **iOS app shell / tabs** — 2 new files
34. **Activity study UI** — 2 new files
35. **Onboarding** — 2 new files
36. **BLE / CoreBluetooth** — 1 new files
37. **Data collection** — 1 new files
38. **Liquid Today surface** — 1 new files
39. **Metal shaders** — 1 new files

Cross-cutting: UI 229 · Logic 264 · Config 0 · Resources 72 (new files)

---

## 1. New files in Aura (565)

### Biscuit companion pet (11)

| Path | Platforms | Description |
|---|---|---|
| `Packages/Biscuit/Package.swift` | iOS, macOS, Android | package |
| `Packages/Biscuit/Sources/Biscuit/BiscuitBehaviorScheduler.swift` | iOS, macOS, Android | biscuit behavior scheduler |
| `Packages/Biscuit/Sources/Biscuit/BiscuitController.swift` | iOS, macOS, Android | biscuit controller |
| `Packages/Biscuit/Sources/Biscuit/BiscuitPose.swift` | iOS, macOS, Android | biscuit pose |
| `Packages/Biscuit/Sources/Biscuit/BiscuitTouch.swift` | iOS, macOS, Android | biscuit touch |
| `Packages/Biscuit/Sources/Biscuit/BiscuitView.swift` | iOS, macOS, Android | biscuit view |
| `Packages/Biscuit/Sources/Biscuit/PersonalityData.swift` | iOS, macOS, Android | personality data |
| `Packages/Biscuit/Sources/Biscuit/PersonalityRenderer.swift` | iOS, macOS, Android | personality renderer |
| `Packages/Biscuit/Sources/Biscuit/PersonalityTimeline.swift` | iOS, macOS, Android | personality timeline |
| `Packages/Biscuit/Sources/Biscuit/Resources/noop-aura.bindings.json` | iOS, macOS, Android | Biscuit vector art bindings |
| `Packages/Biscuit/Sources/Biscuit/Resources/noop-aura.json` | iOS, macOS, Android | Biscuit vector art data |

### Scale (BLE body scale) (40)

| Path | Platforms | Description |
|---|---|---|
| `Packages/ScaleProtocol/Package.swift` | iOS, macOS, Android | package |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/AESCCM.swift` | iOS, macOS, Android | aesccm |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_a_AiLink.swift` | iOS, macOS, Android | batch_a_ai link |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_a_Eufy.swift` | iOS, macOS, Android | batch_a_eufy |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_a_OkOk.swift` | iOS, macOS, Android | batch_a_ok ok |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_a_QN.swift` | iOS, macOS, Android | batch_a_qn |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_a_Support.swift` | iOS, macOS, Android | batch_a_support |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_a_Xiaomi.swift` | iOS, macOS, Android | batch_a_xiaomi |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_a_Yunmai.swift` | iOS, macOS, Android | batch_a_yunmai |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_b_Libs.swift` | iOS, macOS, Android | batch_b_libs |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_b_MiScale.swift` | iOS, macOS, Android | batch_b_mi scale |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_b_OneByone.swift` | iOS, macOS, Android | batch_b_one byone |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_b_Renpho.swift` | iOS, macOS, Android | batch_b_renpho |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_c_Etekcity.swift` | iOS, macOS, Android | batch_c_etekcity |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_c_EufyP2.swift` | iOS, macOS, Android | batch_c_eufy p2 |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_c_QN.swift` | iOS, macOS, Android | batch_c_qn |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_c_Shared.swift` | iOS, macOS, Android | batch_c_shared |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_c_WeightGurus.swift` | iOS, macOS, Android | batch_c_weight gurus |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_c_Yunmai.swift` | iOS, macOS, Android | batch_c_yunmai |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_d_HuaweiAhCh100.swift` | iOS, macOS, Android | batch_d_huawei ah ch100 |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_d_HuaweiHagrid.swift` | iOS, macOS, Android | batch_d_huawei hagrid |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_d_HuaweiHagridWspLib.swift` | iOS, macOS, Android | batch_d_huawei hagrid wsp lib |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_e_Dara.swift` | iOS, macOS, Android | batch_e_dara |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_e_IHealthHs2s.swift` | iOS, macOS, Android | batch_e_i health hs2s |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_e_Picooc.swift` | iOS, macOS, Android | batch_e_picooc |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_e_PicoocWhiteBodyComposition.swift` | iOS, macOS, Android | batch_e_picooc white body composition |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_f_BeurerSanitas.swift` | iOS, macOS, Android | batch_f_beurer sanitas |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_f_StandardBeurerSanitas.swift` | iOS, macOS, Android | batch_f_standard beurer sanitas |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_f_StandardWeight.swift` | iOS, macOS, Android | batch_f_standard weight |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_g_MedisanaBs44x.swift` | iOS, macOS, Android | batch_g_medisana bs44x |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_g_OmronWlc.swift` | iOS, macOS, Android | batch_g_omron wlc |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/Batch_g_Soehnle.swift` | iOS, macOS, Android | batch_g_soehnle |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/Drivers/BroadcastDriversA.swift` | iOS, macOS, Android | broadcast drivers a |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/S400FitCoefficients.swift` | iOS, macOS, Android | s400 fit coefficients |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/S400Formulas.swift` | iOS, macOS, Android | s400 formulas |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/ScaleCatalog.swift` | iOS, macOS, Android | scale catalog |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/ScaleDriver.swift` | iOS, macOS, Android | scale driver |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/ScaleGattDriver.swift` | iOS, macOS, Android | scale gatt driver |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/ScaleSetup.swift` | iOS, macOS, Android | scale setup |
| `Packages/ScaleProtocol/Sources/ScaleProtocol/XiaomiS400Frame.swift` | iOS, macOS, Android | xiaomi s400 frame |

### Analytics math (59)

| Path | Platforms | Description |
|---|---|---|
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ActivityBoutDetector.swift` | iOS, macOS, Android | activity bout detector |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ActivityShapeEngine.swift` | iOS, macOS, Android | activity shape engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ActivityStudyPlan.swift` | iOS, macOS, Android | activity study plan |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/AppOpenModel.swift` | iOS, macOS, Android | app open model |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/BasalFormula.swift` | iOS, macOS, Android | basal formula |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/BestEfforts.swift` | iOS, macOS, Android | best efforts |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/BioAgeInputs.swift` | iOS, macOS, Android | bio age inputs |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/BodyMetrics.swift` | iOS, macOS, Android | body metrics |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/BodyweightTimeline.swift` | iOS, macOS, Android | bodyweight timeline |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/CardioSession.swift` | iOS, macOS, Android | cardio session |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/CircadianTrace.swift` | iOS, macOS, Android | circadian trace |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/CircumferenceProgress.swift` | iOS, macOS, Android | circumference progress |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/DailyRhythm.swift` | iOS, macOS, Android | daily rhythm |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/DayStrainScorer.swift` | iOS, macOS, Android | day strain scorer |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/EffortFeasibility.swift` | iOS, macOS, Android | effort feasibility |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ElevationProfile.swift` | iOS, macOS, Android | elevation profile |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/EnergyCalibrationEngine.swift` | iOS, macOS, Android | energy calibration engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/EnergyEngine.swift` | iOS, macOS, Android | energy engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/EnergyPlanning.swift` | iOS, macOS, Android | energy planning |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/EnergyValidation.swift` | iOS, macOS, Android | energy validation |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/GoalMeasure.swift` | iOS, macOS, Android | goal measure |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/GoalMilestones.swift` | iOS, macOS, Android | goal milestones |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/HRCeilingAlertEngine.swift` | iOS, macOS, Android | hr ceiling alert engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/HRZoneTrainingEngine.swift` | iOS, macOS, Android | hr zone training engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/InBedWake.swift` | iOS, macOS, Android | in bed wake |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/LiftRecords.swift` | iOS, macOS, Android | lift records |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/MaxHeartRate.swift` | iOS, macOS, Android | max heart rate |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/MomentumFeed.swift` | iOS, macOS, Android | momentum feed |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/MuscleBalanceMetric.swift` | iOS, macOS, Android | muscle balance metric |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/MuscleFatigueMetric.swift` | iOS, macOS, Android | muscle fatigue metric |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/MuscleMetricModels.swift` | iOS, macOS, Android | muscle metric models |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/MuscleStimulus.swift` | iOS, macOS, Android | muscle stimulus |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/MuscleStrengthMetric.swift` | iOS, macOS, Android | muscle strength metric |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/NavyBodyFat.swift` | iOS, macOS, Android | navy body fat |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/NoticeRules.swift` | iOS, macOS, Android | notice rules |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/PastSelfComparison.swift` | iOS, macOS, Android | past self comparison |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/PersonalActivityModel.swift` | iOS, macOS, Android | personal activity model |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/PersonalZones.swift` | iOS, macOS, Android | personal zones |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/RRUnitEvidence.swift` | iOS, macOS, Android | rr unit evidence |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepGroupEdit.swift` | iOS, macOS, Android | sleep group edit |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepNeed.swift` | iOS, macOS, Android | sleep need |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepRegularity.swift` | iOS, macOS, Android | sleep regularity |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepSchedule.swift` | iOS, macOS, Android | sleep schedule |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepWindowSettledness.swift` | iOS, macOS, Android | sleep window settledness |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StrengthBalance.swift` | iOS, macOS, Android | strength balance |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StrengthDetail.swift` | iOS, macOS, Android | strength detail |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StrengthOverview.swift` | iOS, macOS, Android | strength overview |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StrengthProgress.swift` | iOS, macOS, Android | strength progress |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StrengthSession.swift` | iOS, macOS, Android | strength session |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StrengthVolume.swift` | iOS, macOS, Android | strength volume |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StrideLength.swift` | iOS, macOS, Android | stride length |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/TrainingActivity.swift` | iOS, macOS, Android | training activity |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/TrainingLoad.swift` | iOS, macOS, Android | training load |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/TrainingLoadSeries.swift` | iOS, macOS, Android | training load series |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/TrainingStatus.swift` | iOS, macOS, Android | training status |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/TrendVerdict.swift` | iOS, macOS, Android | trend verdict |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/WeightTrendSummary.swift` | iOS, macOS, Android | weight trend summary |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/WhoopEnergyModel.swift` | iOS, macOS, Android | whoop energy model |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/WhoopStrain.swift` | iOS, macOS, Android | whoop strain |

### Design system (tokens, charts, icons) (23)

| Path | Platforms | Description |
|---|---|---|
| `Packages/StrandDesign/Sources/StrandDesign/ActivityStatusColors.swift` | iOS, macOS, Android | activity status colors |
| `Packages/StrandDesign/Sources/StrandDesign/AppleInspiredColors.swift` | iOS, macOS, Android | apple inspired colors |
| `Packages/StrandDesign/Sources/StrandDesign/AppleSystemColors.swift` | iOS, macOS, Android | apple system colors |
| `Packages/StrandDesign/Sources/StrandDesign/Aura/NoopChargeGauge.swift` | iOS, macOS, Android | noop charge gauge |
| `Packages/StrandDesign/Sources/StrandDesign/Aura/NoopPalette.swift` | iOS, macOS, Android | noop palette |
| `Packages/StrandDesign/Sources/StrandDesign/Aura/NoopSpecMotion.swift` | iOS, macOS, Android | noop spec motion |
| `Packages/StrandDesign/Sources/StrandDesign/Aura/NoopSpecPrimitives.swift` | iOS, macOS, Android | noop spec primitives |
| `Packages/StrandDesign/Sources/StrandDesign/Aura/NoopSpecTokens.swift` | iOS, macOS, Android | noop spec tokens |
| `Packages/StrandDesign/Sources/StrandDesign/Aura/NoopSpecType.swift` | iOS, macOS, Android | noop spec type |
| `Packages/StrandDesign/Sources/StrandDesign/ChartAudioGraph.swift` | iOS, macOS, Android | chart audio graph |
| `Packages/StrandDesign/Sources/StrandDesign/ChartDifferentiation.swift` | iOS, macOS, Android | chart differentiation |
| `Packages/StrandDesign/Sources/StrandDesign/CoachBreathHalo.swift` | iOS, macOS, Android | coach breath halo |
| `Packages/StrandDesign/Sources/StrandDesign/CoachIconColors.swift` | iOS, macOS, Android | coach icon colors |
| `Packages/StrandDesign/Sources/StrandDesign/LayeredGauge.swift` | iOS, macOS, Android | layered gauge |
| `Packages/StrandDesign/Sources/StrandDesign/MoreRowAppleHealthColors.swift` | iOS, macOS, Android | more row apple health colors |
| `Packages/StrandDesign/Sources/StrandDesign/Resources/Fonts/InstrumentSans.ttf` | iOS, macOS, Android | font: InstrumentSans |
| `Packages/StrandDesign/Sources/StrandDesign/Resources/Fonts/InstrumentSerif-Italic.ttf` | iOS, macOS, Android | font: InstrumentSerif-Italic |
| `Packages/StrandDesign/Sources/StrandDesign/Resources/Fonts/InstrumentSerif-Regular.ttf` | iOS, macOS, Android | font: InstrumentSerif-Regular |
| `Packages/StrandDesign/Sources/StrandDesign/Resources/Fonts/OFL-InstrumentSans.txt` | iOS, macOS, Android | OFL-InstrumentSans.txt |
| `Packages/StrandDesign/Sources/StrandDesign/Resources/Fonts/OFL-InstrumentSerif.txt` | iOS, macOS, Android | OFL-InstrumentSerif.txt |
| `Packages/StrandDesign/Sources/StrandDesign/Resources/Fonts/OFL-Outfit.txt` | iOS, macOS, Android | OFL-Outfit.txt |
| `Packages/StrandDesign/Sources/StrandDesign/Resources/Fonts/Outfit.ttf` | iOS, macOS, Android | font: Outfit |
| `Packages/StrandDesign/Sources/StrandDesign/SettingsIconColors.swift` | iOS, macOS, Android | settings icon colors |

### Fitness engine (cardio, HR zones, importers) (14)

| Path | Platforms | Description |
|---|---|---|
| `Packages/StrandFitness/Package.swift` | iOS, macOS, Android | package |
| `Packages/StrandFitness/Sources/StrandFitness/ActivityStreak.swift` | iOS, macOS, Android | activity streak |
| `Packages/StrandFitness/Sources/StrandFitness/AcuteChronicLoad.swift` | iOS, macOS, Android | acute chronic load |
| `Packages/StrandFitness/Sources/StrandFitness/CardioMetrics.swift` | iOS, macOS, Android | cardio metrics |
| `Packages/StrandFitness/Sources/StrandFitness/FitnessInputs.swift` | iOS, macOS, Android | fitness inputs |
| `Packages/StrandFitness/Sources/StrandFitness/MuscleFreshness.swift` | iOS, macOS, Android | muscle freshness |
| `Packages/StrandFitness/Sources/StrandFitness/MuscularLoad.swift` | iOS, macOS, Android | muscular load |
| `Packages/StrandFitness/Sources/StrandFitness/PeriodGoal.swift` | iOS, macOS, Android | period goal |
| `Packages/StrandFitness/Sources/StrandFitness/Rewards.swift` | iOS, macOS, Android | rewards |
| `Packages/StrandFitness/Sources/StrandFitness/SportDetails.swift` | iOS, macOS, Android | sport details |
| `Packages/StrandFitness/Sources/StrandFitness/SportMuscles.swift` | iOS, macOS, Android | sport muscles |
| `Packages/StrandFitness/Sources/StrandFitness/StreakChallenge.swift` | iOS, macOS, Android | streak challenge |
| `Packages/StrandFitness/Sources/StrandFitness/TrainingStreak.swift` | iOS, macOS, Android | training streak |
| `Packages/StrandFitness/Sources/StrandFitness/WeeklyQuests.swift` | iOS, macOS, Android | weekly quests |

### Importers (Hevy, Strava, Mi, Google, Health) (6)

| Path | Platforms | Description |
|---|---|---|
| `Packages/StrandImport/Sources/StrandImport/BrandExportReader.swift` | iOS, macOS, Android | brand export reader |
| `Packages/StrandImport/Sources/StrandImport/GoogleHealthExportReader.swift` | iOS, macOS, Android | google health export reader |
| `Packages/StrandImport/Sources/StrandImport/LabReportTextImport.swift` | iOS, macOS, Android | lab report text import |
| `Packages/StrandImport/Sources/StrandImport/LockedZip.swift` | iOS, macOS, Android | locked zip |
| `Packages/StrandImport/Sources/StrandImport/MiFitnessExportReader.swift` | iOS, macOS, Android | mi fitness export reader |
| `Packages/StrandImport/Sources/StrandImport/TCXExporter.swift` | iOS, macOS, Android | tcx exporter |

### Training engine (workouts/programs/strength) (22)

| Path | Platforms | Description |
|---|---|---|
| `Packages/StrandTraining/Package.swift` | iOS, macOS, Android | package |
| `Packages/StrandTraining/Sources/StrandTraining/AuraExerciseCatalog.swift` | iOS, macOS, Android | aura exercise catalog |
| `Packages/StrandTraining/Sources/StrandTraining/BundledExerciseCatalog.swift` | iOS, macOS, Android | bundled exercise catalog |
| `Packages/StrandTraining/Sources/StrandTraining/ExerciseAnatomyCatalog.swift` | iOS, macOS, Android | exercise anatomy catalog |
| `Packages/StrandTraining/Sources/StrandTraining/ExerciseContent.swift` | iOS, macOS, Android | exercise content |
| `Packages/StrandTraining/Sources/StrandTraining/ExerciseMediaPolicy.swift` | iOS, macOS, Android | exercise media policy |
| `Packages/StrandTraining/Sources/StrandTraining/PlateCalculator.swift` | iOS, macOS, Android | plate calculator |
| `Packages/StrandTraining/Sources/StrandTraining/ProgramGenerator.swift` | iOS, macOS, Android | program generator |
| `Packages/StrandTraining/Sources/StrandTraining/ProgressionEngine.swift` | iOS, macOS, Android | progression engine |
| `Packages/StrandTraining/Sources/StrandTraining/Resources/exercise-catalog.json` | iOS, macOS, Android | JSON data: exercise-catalog |
| `Packages/StrandTraining/Sources/StrandTraining/RoutineEditing.swift` | iOS, macOS, Android | routine editing |
| `Packages/StrandTraining/Sources/StrandTraining/RoutineScheduleEntry.swift` | iOS, macOS, Android | routine schedule entry |
| `Packages/StrandTraining/Sources/StrandTraining/TrainingCatalog.swift` | iOS, macOS, Android | training catalog |
| `Packages/StrandTraining/Sources/StrandTraining/TrainingEquipmentCatalog.swift` | iOS, macOS, Android | training equipment catalog |
| `Packages/StrandTraining/Sources/StrandTraining/TrainingExchange.swift` | iOS, macOS, Android | training exchange |
| `Packages/StrandTraining/Sources/StrandTraining/TrainingModels.swift` | iOS, macOS, Android | training models |
| `Packages/StrandTraining/Sources/StrandTraining/TrainingPlan.swift` | iOS, macOS, Android | training plan |
| `Packages/StrandTraining/Sources/StrandTraining/TrainingSources.swift` | iOS, macOS, Android | training sources |
| `Packages/StrandTraining/Sources/StrandTraining/WarmupPlanner.swift` | iOS, macOS, Android | warmup planner |
| `Packages/StrandTraining/Sources/StrandTraining/WeightIncrement.swift` | iOS, macOS, Android | weight increment |
| `Packages/StrandTraining/Sources/StrandTraining/WorkoutEngine.swift` | iOS, macOS, Android | workout engine |
| `Packages/StrandTraining/Sources/StrandTraining/WorkoutTimerCoordinator.swift` | iOS, macOS, Android | workout timer coordinator |

### WHOOP protocol (3)

| Path | Platforms | Description |
|---|---|---|
| `Packages/WhoopProtocol/Sources/WhoopProtocol/AuraRelease.swift` | iOS, macOS, Android | aura release |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/AuraReleaseNotes.swift` | iOS, macOS, Android | aura release notes |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/WhoopProtocol.swift` | iOS, macOS, Android | whoop protocol |

### Storage (WhoopStore/GRDB) (9)

| Path | Platforms | Description |
|---|---|---|
| `Packages/WhoopStore/Sources/WhoopStore/AnalysisInputRevision.swift` | iOS, macOS, Android | analysis input revision |
| `Packages/WhoopStore/Sources/WhoopStore/BackupAuraState.swift` | iOS, macOS, Android | backup aura state |
| `Packages/WhoopStore/Sources/WhoopStore/BackupProgressPhotos.swift` | iOS, macOS, Android | backup progress photos |
| `Packages/WhoopStore/Sources/WhoopStore/BodyLogStore.swift` | iOS, macOS, Android | body log store |
| `Packages/WhoopStore/Sources/WhoopStore/DayScanFingerprint.swift` | iOS, macOS, Android | day scan fingerprint |
| `Packages/WhoopStore/Sources/WhoopStore/HevyModels.swift` | iOS, macOS, Android | hevy models |
| `Packages/WhoopStore/Sources/WhoopStore/LogEntryStore.swift` | iOS, macOS, Android | log entry store |
| `Packages/WhoopStore/Sources/WhoopStore/SveaBackupBundle.swift` | iOS, macOS, Android | svea backup bundle |
| `Packages/WhoopStore/Sources/WhoopStore/WorkoutFeelStore.swift` | iOS, macOS, Android | workout feel store |

### AI coach "Svea" (50)

| Path | Platforms | Description |
|---|---|---|
| `Strand/AI/AICoachUpstreamPorts.swift` | iOS, macOS, Android | ai coach upstream ports |
| `Strand/AI/CoachAccuracy.swift` | iOS, macOS, Android | coach accuracy |
| `Strand/AI/CoachBriefStamp.swift` | iOS, macOS, Android | coach brief stamp |
| `Strand/AI/CoachCardContext.swift` | iOS, macOS, Android | coach card context |
| `Strand/AI/CoachChart.swift` | iOS, macOS, Android | coach chart |
| `Strand/AI/CoachDataCatalog.swift` | iOS, macOS, Android | coach data catalog |
| `Strand/AI/CoachGoal.swift` | iOS, macOS, Android | coach goal |
| `Strand/AI/CoachGoalNudgeStamps.swift` | iOS, macOS, Android | coach goal nudge stamps |
| `Strand/AI/CoachGoalSetupProposal.swift` | iOS, macOS, Android | coach goal setup proposal |
| `Strand/AI/CoachGoalSetupTool.swift` | iOS, macOS, Android | coach goal setup tool |
| `Strand/AI/CoachHistoryBudget.swift` | iOS, macOS, Android | coach history budget |
| `Strand/AI/CoachIdentity.swift` | iOS, macOS, Android | coach identity |
| `Strand/AI/CoachJournalPatternAnalyzer.swift` | iOS, macOS, Android | coach journal pattern analyzer |
| `Strand/AI/CoachLocalContextPlanner.swift` | iOS, macOS, Android | coach local context planner |
| `Strand/AI/CoachLocalQueryRouter.swift` | iOS, macOS, Android | coach local query router |
| `Strand/AI/CoachMemory.swift` | iOS, macOS, Android | coach memory |
| `Strand/AI/CoachMemoryFootprint.swift` | iOS, macOS, Android | coach memory footprint |
| `Strand/AI/CoachMetricHistory.swift` | iOS, macOS, Android | coach metric history |
| `Strand/AI/CoachModelWatch.swift` | iOS, macOS, Android | coach model watch |
| `Strand/AI/CoachNotifier.swift` | iOS, macOS, Android | coach notifier |
| `Strand/AI/CoachPendingWrite.swift` | iOS, macOS, Android | coach pending write |
| `Strand/AI/CoachPersona.swift` | iOS, macOS, Android | coach persona |
| `Strand/AI/CoachPlanStore.swift` | iOS, macOS, Android | coach plan store |
| `Strand/AI/CoachPreset.swift` | iOS, macOS, Android | coach preset |
| `Strand/AI/CoachQuestionRouting.swift` | iOS, macOS, Android | coach question routing |
| `Strand/AI/CoachRecommendationAdaptation.swift` | iOS, macOS, Android | coach recommendation adaptation |
| `Strand/AI/CoachReplyLanguage.swift` | iOS, macOS, Android | coach reply language |
| `Strand/AI/CoachReplyMarkdown.swift` | iOS, macOS, Android | coach reply markdown |
| `Strand/AI/CoachStreaming.swift` | iOS, macOS, Android | coach streaming |
| `Strand/AI/CoachTools.swift` | iOS, macOS, Android | coach tools |
| `Strand/AI/CoachTrainingPreferences.swift` | iOS, macOS, Android | coach training preferences |
| `Strand/AI/CoachTranscriptStore.swift` | iOS, macOS, Android | coach transcript store |
| `Strand/AI/CoachUsageLog.swift` | iOS, macOS, Android | coach usage log |
| `Strand/AI/CoachVerbosity.swift` | iOS, macOS, Android | coach verbosity |
| `Strand/AI/GoalActions.swift` | iOS, macOS, Android | goal actions |
| `Strand/AI/GoalFeasibility.swift` | iOS, macOS, Android | goal feasibility |
| `Strand/AI/GoalOnboardingDraft.swift` | iOS, macOS, Android | goal onboarding draft |
| `Strand/AI/GoalSafetyGate.swift` | iOS, macOS, Android | goal safety gate |
| `Strand/AI/GoalTrackingEngine.swift` | iOS, macOS, Android | goal tracking engine |
| `Strand/AI/GoalVolumeGate.swift` | iOS, macOS, Android | goal volume gate |
| `Strand/AI/JournalDuplicateReviewer.swift` | iOS, macOS, Android | journal duplicate reviewer |
| `Strand/AI/JourneyExplain.swift` | iOS, macOS, Android | journey explain |
| `Strand/AI/JourneyMilestones.swift` | iOS, macOS, Android | journey milestones |
| `Strand/AI/MemoryMaintainer.swift` | iOS, macOS, Android | memory maintainer |
| `Strand/AI/PlanConsequence.swift` | iOS, macOS, Android | plan consequence |
| `Strand/AI/PlanReminder.swift` | iOS, macOS, Android | plan reminder |
| `Strand/AI/PlanWorkoutMatcher.swift` | iOS, macOS, Android | plan workout matcher |
| `Strand/AI/ProactiveCoach.swift` | iOS, macOS, Android | proactive coach |
| `Strand/AI/SveaDataGrants.swift` | iOS, macOS, Android | svea data grants |
| `Strand/AI/ToolConsent.swift` | iOS, macOS, Android | tool consent |

### AI providers (Svea) (9)

| Path | Platforms | Description |
|---|---|---|
| `Strand/AI/Providers/AnthropicCaching.swift` | iOS, macOS, Android | anthropic caching |
| `Strand/AI/Providers/AnthropicStreaming.swift` | iOS, macOS, Android | anthropic streaming |
| `Strand/AI/Providers/AnthropicTools.swift` | iOS, macOS, Android | anthropic tools |
| `Strand/AI/Providers/AppleOnDevice.swift` | iOS, macOS, Android | apple on device |
| `Strand/AI/Providers/CoachOutputBudget.swift` | iOS, macOS, Android | coach output budget |
| `Strand/AI/Providers/GeminiTools.swift` | iOS, macOS, Android | gemini tools |
| `Strand/AI/Providers/OpenAICompatibleStreaming.swift` | iOS, macOS, Android | open ai compatible streaming |
| `Strand/AI/Providers/OpenAICompatibleTools.swift` | iOS, macOS, Android | open ai compatible tools |
| `Strand/AI/Providers/OpenRouter.swift` | iOS, macOS, Android | open router |

### App shell / root views (2)

| Path | Platforms | Description |
|---|---|---|
| `Strand/App/RouteSplits.swift` | iOS, macOS, Android | route splits |
| `Strand/App/TermsDocument.swift` | iOS, macOS, Android | terms document |

### BLE / CoreBluetooth (1)

| Path | Platforms | Description |
|---|---|---|
| `Strand/BLE/LiveHeartRateResubscribe.swift` | iOS, macOS, Android | live heart rate resubscribe |

### Data collection (1)

| Path | Platforms | Description |
|---|---|---|
| `Strand/Collect/BackfillDrain.swift` | iOS, macOS, Android | backfill drain |

### Data layer (Repository) (21)

| Path | Platforms | Description |
|---|---|---|
| `Strand/Data/ActivityStatus.swift` | iOS, macOS, Android | activity status |
| `Strand/Data/AuraHistoryPresentation.swift` | iOS, macOS, Android | aura history presentation |
| `Strand/Data/BaseCardStatement.swift` | iOS, macOS, Android | base card statement |
| `Strand/Data/DatedChartSeries.swift` | iOS, macOS, Android | dated chart series |
| `Strand/Data/HevySync.swift` | iOS, macOS, Android | hevy sync |
| `Strand/Data/HistoryDateWindow.swift` | iOS, macOS, Android | history date window |
| `Strand/Data/IntelligenceEngine+MaxHRPeaks.swift` | iOS, macOS, Android | intelligence engine+max hr peaks |
| `Strand/Data/JournalMerge.swift` | iOS, macOS, Android | journal merge |
| `Strand/Data/LabReportImageTextExtractor.swift` | iOS, macOS, Android | lab report image text extractor |
| `Strand/Data/LabReportPDFTextExtractor.swift` | iOS, macOS, Android | lab report pdf text extractor |
| `Strand/Data/LiftCoachNote.swift` | iOS, macOS, Android | lift coach note |
| `Strand/Data/LogBook.swift` | iOS, macOS, Android | log book |
| `Strand/Data/LogCatalog.swift` | iOS, macOS, Android | log catalog |
| `Strand/Data/LogInput.swift` | iOS, macOS, Android | log input |
| `Strand/Data/PersonalZonesModel.swift` | iOS, macOS, Android | personal zones model |
| `Strand/Data/Repository+ActivityDetection.swift` | iOS, macOS, Android | repository+activity detection |
| `Strand/Data/SearchMatch.swift` | iOS, macOS, Android | search match |
| `Strand/Data/ShortcutSessionExport.swift` | iOS, macOS, Android | shortcut session export |
| `Strand/Data/SleepNeedPrefs.swift` | iOS, macOS, Android | sleep need prefs |
| `Strand/Data/SleepSchedulePrefs.swift` | iOS, macOS, Android | sleep schedule prefs |
| `Strand/Data/SyncReminderPolicy.swift` | iOS, macOS, Android | sync reminder policy |

### Liquid Today surface (1)

| Path | Platforms | Description |
|---|---|---|
| `Strand/Liquid/LiquidLiveHR.swift` | iOS, macOS, Android | liquid live hr |

### App resources (assets, plist, entitlements) (4)

| Path | Platforms | Description |
|---|---|---|
| `Strand/Resources/Assets.xcassets/marv-avatar.imageset/Contents.json` | iOS, macOS | JSON data: Contents |
| `Strand/Resources/Assets.xcassets/marv-avatar.imageset/marv-avatar.png` | iOS, macOS | image asset: marv-avatar |
| `Strand/Resources/Assets.xcassets/svea-avatar.imageset/Contents.json` | iOS, macOS | JSON data: Contents |
| `Strand/Resources/Assets.xcassets/svea-avatar.imageset/svea-avatar.png` | iOS, macOS | image asset: svea-avatar |

### Screens (shared iOS+macOS) (25)

| Path | Platforms | Description |
|---|---|---|
| `Strand/Screens/ActivityStatusSheet+Shared.swift` | iOS, macOS, Android | activity status sheet+shared |
| `Strand/Screens/CoachAvatarView.swift` | iOS, macOS, Android | coach avatar view |
| `Strand/Screens/CoachEntry.swift` | iOS, macOS, Android | coach entry |
| `Strand/Screens/CoachGoalJourneyView.swift` | iOS, macOS, Android | coach goal journey view |
| `Strand/Screens/CoachGoalOnboardingFlow.swift` | iOS, macOS, Android | coach goal onboarding flow |
| `Strand/Screens/CoachGoalSetupReviewView.swift` | iOS, macOS, Android | coach goal setup review view |
| `Strand/Screens/CoachGoalView.swift` | iOS, macOS, Android | coach goal view |
| `Strand/Screens/CoachHistoryView.swift` | iOS, macOS, Android | coach history view |
| `Strand/Screens/CoachIdentityEditor.swift` | iOS, macOS, Android | coach identity editor |
| `Strand/Screens/CoachInfoView.swift` | iOS, macOS, Android | coach info view |
| `Strand/Screens/CoachPlanView.swift` | iOS, macOS, Android | coach plan view |
| `Strand/Screens/CoachStreamingText.swift` | iOS, macOS, Android | coach streaming text |
| `Strand/Screens/CoachSurfaceShared.swift` | iOS, macOS, Android | coach surface shared |
| `Strand/Screens/GoalActionsView.swift` | iOS, macOS, Android | goal actions view |
| `Strand/Screens/GoalSummaryLines.swift` | iOS, macOS, Android | goal summary lines |
| `Strand/Screens/GoalTrackingTile.swift` | iOS, macOS, Android | goal tracking tile |
| `Strand/Screens/GoalWeekGrid.swift` | iOS, macOS, Android | goal week grid |
| `Strand/Screens/HRZoneEditorView.swift` | iOS, macOS, Android | hr zone editor view |
| `Strand/Screens/JourneyView.swift` | iOS, macOS, Android | journey view |
| `Strand/Screens/LabReportReviewView.swift` | iOS, macOS, Android | lab report review view |
| `Strand/Screens/MilestoneEditorSheet.swift` | iOS, macOS, Android | milestone editor sheet |
| `Strand/Screens/ModelSearchSheet.swift` | iOS, macOS, Android | model search sheet |
| `Strand/Screens/MorningSuggestionCard.swift` | iOS, macOS, Android | morning suggestion card |
| `Strand/Screens/PlanTodayCard.swift` | iOS, macOS, Android | plan today card |
| `Strand/Screens/SettingsSearchCatalog.swift` | iOS, macOS, Android | settings search catalog |

### System services (notifications, updates, background) (9)

| Path | Platforms | Description |
|---|---|---|
| `Strand/System/ActivityNotifier.swift` | iOS, macOS, Android | activity notifier |
| `Strand/System/AlertInbox.swift` | iOS, macOS, Android | alert inbox |
| `Strand/System/AuraNotices.swift` | iOS, macOS, Android | aura notices |
| `Strand/System/CoachCheckIn.swift` | iOS, macOS, Android | coach check in |
| `Strand/System/LocalizedCatalogValue.swift` | iOS, macOS, Android | localized catalog value |
| `Strand/System/PhoneActivity.swift` | iOS, macOS, Android | phone activity |
| `Strand/System/SyncReminder.swift` | iOS, macOS, Android | sync reminder |
| `Strand/System/WakeWindow.swift` | iOS, macOS, Android | wake window |
| `Strand/System/WearNotifier.swift` | iOS, macOS, Android | wear notifier |

### AI background tasks (iOS) (3)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/AI/ModelWatchBackgroundTask.swift` | iOS, Android | model watch background task |
| `StrandiOS/AI/NoopLogModels.swift` | iOS, Android | noop log models |
| `StrandiOS/AI/SveaProactiveBackgroundTask.swift` | iOS, Android | svea proactive background task |

### iOS app shell / tabs (2)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/App/MoreCatalog.swift` | iOS, Android | more catalog |
| `StrandiOS/App/NoopMetricReports.swift` | iOS, Android | noop metric reports |

### HealthKit (iOS) (4)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/Health/HealthKitBridge+AllTypes.swift` | iOS, Android | health kit bridge+all types |
| `StrandiOS/Health/HealthKitBridge+BioAge.swift` | iOS, Android | health kit bridge+bio age |
| `StrandiOS/Health/HealthWritebackDiagnostics.swift` | iOS, Android | health writeback diagnostics |
| `StrandiOS/Health/WorkoutFocusSession.swift` | iOS, Android | workout focus session |

### Activity study UI (2)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/NoopUI/ActivityStudy/NoopActivityStudy.swift` | iOS, Android | noop activity study |
| `StrandiOS/NoopUI/ActivityStudy/NoopActivityStudyViews.swift` | iOS, Android | noop activity study views |

### Acts staged screens (40)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/NoopUI/Acts/NoopAct10Lift.swift` | iOS, Android | noop act10 lift |
| `StrandiOS/NoopUI/Acts/NoopAct1Night.swift` | iOS, Android | noop act1 night |
| `StrandiOS/NoopUI/Acts/NoopAct1RestRecord.swift` | iOS, Android | noop act1 rest record |
| `StrandiOS/NoopUI/Acts/NoopAct2Day.swift` | iOS, Android | noop act2 day |
| `StrandiOS/NoopUI/Acts/NoopAct2DayRecord.swift` | iOS, Android | noop act2 day record |
| `StrandiOS/NoopUI/Acts/NoopAct3Effort.swift` | iOS, Android | noop act3 effort |
| `StrandiOS/NoopUI/Acts/NoopAct3EffortRecord.swift` | iOS, Android | noop act3 effort record |
| `StrandiOS/NoopUI/Acts/NoopAct4Trends.swift` | iOS, Android | noop act4 trends |
| `StrandiOS/NoopUI/Acts/NoopAct4TrendsRecord.swift` | iOS, Android | noop act4 trends record |
| `StrandiOS/NoopUI/Acts/NoopAct5Data.swift` | iOS, Android | noop act5 data |
| `StrandiOS/NoopUI/Acts/NoopAct5Utilities.swift` | iOS, Android | noop act5 utilities |
| `StrandiOS/NoopUI/Acts/NoopAct5You.swift` | iOS, Android | noop act5 you |
| `StrandiOS/NoopUI/Acts/NoopAct5YouRecord.swift` | iOS, Android | noop act5 you record |
| `StrandiOS/NoopUI/Acts/NoopAct6Ages.swift` | iOS, Android | noop act6 ages |
| `StrandiOS/NoopUI/Acts/NoopAct6AgesRecord.swift` | iOS, Android | noop act6 ages record |
| `StrandiOS/NoopUI/Acts/NoopAct7Svea.swift` | iOS, Android | noop act7 svea |
| `StrandiOS/NoopUI/Acts/NoopAct8Goals.swift` | iOS, Android | noop act8 goals |
| `StrandiOS/NoopUI/Acts/NoopAct9Instrument.swift` | iOS, Android | noop act9 instrument |
| `StrandiOS/NoopUI/Acts/NoopAuraAbout.swift` | iOS, Android | noop aura about |
| `StrandiOS/NoopUI/Acts/NoopAuraOrb.swift` | iOS, Android | noop aura orb |
| `StrandiOS/NoopUI/Acts/NoopDailyRhythmScreen.swift` | iOS, Android | noop daily rhythm screen |
| `StrandiOS/NoopUI/Acts/NoopDataExports.swift` | iOS, Android | noop data exports |
| `StrandiOS/NoopUI/Acts/NoopHeroFamilies.swift` | iOS, Android | noop hero families |
| `StrandiOS/NoopUI/Acts/NoopHeroes.swift` | iOS, Android | noop heroes |
| `StrandiOS/NoopUI/Acts/NoopRestDetailSlots.swift` | iOS, Android | noop rest detail slots |
| `StrandiOS/NoopUI/Acts/NoopRestSlots.swift` | iOS, Android | noop rest slots |
| `StrandiOS/NoopUI/Acts/NoopRings.swift` | iOS, Android | noop rings |
| `StrandiOS/NoopUI/Acts/NoopScorePages.swift` | iOS, Android | noop score pages |
| `StrandiOS/NoopUI/Acts/NoopShortcutsGuide.swift` | iOS, Android | noop shortcuts guide |
| `StrandiOS/NoopUI/Acts/NoopSleepLanes.swift` | iOS, Android | noop sleep lanes |
| `StrandiOS/NoopUI/Acts/NoopStressSheet.swift` | iOS, Android | noop stress sheet |
| `StrandiOS/NoopUI/Acts/NoopSveaReply.swift` | iOS, Android | noop svea reply |
| `StrandiOS/NoopUI/Acts/NoopTodayCards.swift` | iOS, Android | noop today cards |
| `StrandiOS/NoopUI/Acts/NoopTodaySlots.swift` | iOS, Android | noop today slots |
| `StrandiOS/NoopUI/Acts/NoopTodayTileSheets.swift` | iOS, Android | noop today tile sheets |
| `StrandiOS/NoopUI/Acts/NoopTodayTiles.swift` | iOS, Android | noop today tiles |
| `StrandiOS/NoopUI/Acts/NoopTrendsHero.swift` | iOS, Android | noop trends hero |
| `StrandiOS/NoopUI/Acts/NoopTrendsMarkers.swift` | iOS, Android | noop trends markers |
| `StrandiOS/NoopUI/Acts/NoopTrendsThenNow.swift` | iOS, Android | noop trends then now |
| `StrandiOS/NoopUI/Acts/NoopZonesScreen.swift` | iOS, Android | noop zones screen |

### Companion (Biscuit/Svea) UI (3)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/NoopUI/Companion/NoopAIAgreement.swift` | iOS, Android | noop ai agreement |
| `StrandiOS/NoopUI/Companion/NoopBiscuit.swift` | iOS, Android | noop biscuit |
| `StrandiOS/NoopUI/Companion/NoopCompanionLab.swift` | iOS, Android | noop companion lab |

### Fitness UI (43)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/NoopUI/Fitness/ExerciseMediaStore.swift` | iOS, Android | exercise media store |
| `StrandiOS/NoopUI/Fitness/NoopActivityConfirmSheet.swift` | iOS, Android | noop activity confirm sheet |
| `StrandiOS/NoopUI/Fitness/NoopBestEfforts.swift` | iOS, Android | noop best efforts |
| `StrandiOS/NoopUI/Fitness/NoopCardioGoal.swift` | iOS, Android | noop cardio goal |
| `StrandiOS/NoopUI/Fitness/NoopCardioSummary.swift` | iOS, Android | noop cardio summary |
| `StrandiOS/NoopUI/Fitness/NoopElevationCard.swift` | iOS, Android | noop elevation card |
| `StrandiOS/NoopUI/Fitness/NoopFinishParts.swift` | iOS, Android | noop finish parts |
| `StrandiOS/NoopUI/Fitness/NoopFitnessAnatomy.swift` | iOS, Android | noop fitness anatomy |
| `StrandiOS/NoopUI/Fitness/NoopFitnessBody.swift` | iOS, Android | noop fitness body |
| `StrandiOS/NoopUI/Fitness/NoopFitnessCalendar.swift` | iOS, Android | noop fitness calendar |
| `StrandiOS/NoopUI/Fitness/NoopFitnessCardioLive.swift` | iOS, Android | noop fitness cardio live |
| `StrandiOS/NoopUI/Fitness/NoopFitnessData.swift` | iOS, Android | noop fitness data |
| `StrandiOS/NoopUI/Fitness/NoopFitnessDayStrip.swift` | iOS, Android | noop fitness day strip |
| `StrandiOS/NoopUI/Fitness/NoopFitnessDetailPages.swift` | iOS, Android | noop fitness detail pages |
| `StrandiOS/NoopUI/Fitness/NoopFitnessDetails.swift` | iOS, Android | noop fitness details |
| `StrandiOS/NoopUI/Fitness/NoopFitnessExerciseDetail.swift` | iOS, Android | noop fitness exercise detail |
| `StrandiOS/NoopUI/Fitness/NoopFitnessExerciseInsights.swift` | iOS, Android | noop fitness exercise insights |
| `StrandiOS/NoopUI/Fitness/NoopFitnessHome.swift` | iOS, Android | noop fitness home |
| `StrandiOS/NoopUI/Fitness/NoopFitnessInstrument.swift` | iOS, Android | noop fitness instrument |
| `StrandiOS/NoopUI/Fitness/NoopFitnessPlanning.swift` | iOS, Android | noop fitness planning |
| `StrandiOS/NoopUI/Fitness/NoopFitnessProgramBuilder.swift` | iOS, Android | noop fitness program builder |
| `StrandiOS/NoopUI/Fitness/NoopFitnessReport.swift` | iOS, Android | noop fitness report |
| `StrandiOS/NoopUI/Fitness/NoopFitnessRoutineEditor.swift` | iOS, Android | noop fitness routine editor |
| `StrandiOS/NoopUI/Fitness/NoopFitnessRoutineGrid.swift` | iOS, Android | noop fitness routine grid |
| `StrandiOS/NoopUI/Fitness/NoopFitnessRoutineProgress.swift` | iOS, Android | noop fitness routine progress |
| `StrandiOS/NoopUI/Fitness/NoopFitnessRoutineSheets.swift` | iOS, Android | noop fitness routine sheets |
| `StrandiOS/NoopUI/Fitness/NoopFitnessSheets.swift` | iOS, Android | noop fitness sheets |
| `StrandiOS/NoopUI/Fitness/NoopFitnessSports.swift` | iOS, Android | noop fitness sports |
| `StrandiOS/NoopUI/Fitness/NoopFitnessWeekMix.swift` | iOS, Android | noop fitness week mix |
| `StrandiOS/NoopUI/Fitness/NoopGear.swift` | iOS, Android | noop gear |
| `StrandiOS/NoopUI/Fitness/NoopHeatmap.swift` | iOS, Android | noop heatmap |
| `StrandiOS/NoopUI/Fitness/NoopHevyScreen.swift` | iOS, Android | noop hevy screen |
| `StrandiOS/NoopUI/Fitness/NoopManualWorkoutSheet.swift` | iOS, Android | noop manual workout sheet |
| `StrandiOS/NoopUI/Fitness/NoopMeasureNow.swift` | iOS, Android | noop measure now |
| `StrandiOS/NoopUI/Fitness/NoopScaleData.swift` | iOS, Android | noop scale data |
| `StrandiOS/NoopUI/Fitness/NoopScaleListener.swift` | iOS, Android | noop scale listener |
| `StrandiOS/NoopUI/Fitness/NoopScalePicker.swift` | iOS, Android | noop scale picker |
| `StrandiOS/NoopUI/Fitness/NoopScaleRunner.swift` | iOS, Android | noop scale runner |
| `StrandiOS/NoopUI/Fitness/NoopSessionParts.swift` | iOS, Android | noop session parts |
| `StrandiOS/NoopUI/Fitness/NoopSportDetails.swift` | iOS, Android | noop sport details |
| `StrandiOS/NoopUI/Fitness/NoopStravaScreen.swift` | iOS, Android | noop strava screen |
| `StrandiOS/NoopUI/Fitness/NoopWorkoutList.swift` | iOS, Android | noop workout list |
| `StrandiOS/NoopUI/Fitness/NoopYearInSport.swift` | iOS, Android | noop year in sport |

### Goals UI (14)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/NoopUI/Goals/NoopChallengeParts.swift` | iOS, Android | noop challenge parts |
| `StrandiOS/NoopUI/Goals/NoopChestReveal.swift` | iOS, Android | noop chest reveal |
| `StrandiOS/NoopUI/Goals/NoopCollectibles.swift` | iOS, Android | noop collectibles |
| `StrandiOS/NoopUI/Goals/NoopFlame.metal` | iOS, Android | NoopFlame.metal |
| `StrandiOS/NoopUI/Goals/NoopGoalsParts.swift` | iOS, Android | noop goals parts |
| `StrandiOS/NoopUI/Goals/NoopGoalsPrompt.swift` | iOS, Android | noop goals prompt |
| `StrandiOS/NoopUI/Goals/NoopGoalsScreen.swift` | iOS, Android | noop goals screen |
| `StrandiOS/NoopUI/Goals/NoopGoalsStore.swift` | iOS, Android | noop goals store |
| `StrandiOS/NoopUI/Goals/NoopQuestParts.swift` | iOS, Android | noop quest parts |
| `StrandiOS/NoopUI/Goals/NoopRewards.swift` | iOS, Android | noop rewards |
| `StrandiOS/NoopUI/Goals/NoopRiveChest.swift` | iOS, Android | noop rive chest |
| `StrandiOS/NoopUI/Goals/NoopStreakIntro.swift` | iOS, Android | noop streak intro |
| `StrandiOS/NoopUI/Goals/NoopStreakMoment.swift` | iOS, Android | noop streak moment |
| `StrandiOS/NoopUI/Goals/NoopTierFlame.swift` | iOS, Android | noop tier flame |

### Journal/logging UI (8)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/NoopUI/Logging/NoopJournalCustomize.swift` | iOS, Android | noop journal customize |
| `StrandiOS/NoopUI/Logging/NoopJournalScreen.swift` | iOS, Android | noop journal screen |
| `StrandiOS/NoopUI/Logging/NoopLogActivities.swift` | iOS, Android | noop log activities |
| `StrandiOS/NoopUI/Logging/NoopLogQuick.swift` | iOS, Android | noop log quick |
| `StrandiOS/NoopUI/Logging/NoopPageTimeline.swift` | iOS, Android | noop page timeline |
| `StrandiOS/NoopUI/Logging/NoopStacks.swift` | iOS, Android | noop stacks |
| `StrandiOS/NoopUI/Logging/NoopTimelineCard.swift` | iOS, Android | noop timeline card |
| `StrandiOS/NoopUI/Logging/NoopTimelineFace.swift` | iOS, Android | noop timeline face |

### iOS UI kit (NoopUI) (29)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/NoopUI/NoopActivityOffer.swift` | iOS, Android | noop activity offer |
| `StrandiOS/NoopUI/NoopActivityStatus.swift` | iOS, Android | noop activity status |
| `StrandiOS/NoopUI/NoopAmbientLayers.swift` | iOS, Android | noop ambient layers |
| `StrandiOS/NoopUI/NoopBackSwipe.swift` | iOS, Android | noop back swipe |
| `StrandiOS/NoopUI/NoopChartInteraction.swift` | iOS, Android | noop chart interaction |
| `StrandiOS/NoopUI/NoopDayPicker.swift` | iOS, Android | noop day picker |
| `StrandiOS/NoopUI/NoopDefaults.swift` | iOS, Android | noop defaults |
| `StrandiOS/NoopUI/NoopDesign.swift` | iOS, Android | noop design |
| `StrandiOS/NoopUI/NoopHRVReadinessCard.swift` | iOS, Android | noop hrv readiness card |
| `StrandiOS/NoopUI/NoopHelixHero.swift` | iOS, Android | noop helix hero |
| `StrandiOS/NoopUI/NoopHistoryPlot.swift` | iOS, Android | noop history plot |
| `StrandiOS/NoopUI/NoopImportHub.swift` | iOS, Android | noop import hub |
| `StrandiOS/NoopUI/NoopLiveGlance.swift` | iOS, Android | noop live glance |
| `StrandiOS/NoopUI/NoopLocalReset.swift` | iOS, Android | noop local reset |
| `StrandiOS/NoopUI/NoopMarkerNorms.swift` | iOS, Android | noop marker norms |
| `StrandiOS/NoopUI/NoopMetricSheet.swift` | iOS, Android | noop metric sheet |
| `StrandiOS/NoopUI/NoopNavigation.swift` | iOS, Android | noop navigation |
| `StrandiOS/NoopUI/NoopPageLayout.swift` | iOS, Android | noop page layout |
| `StrandiOS/NoopUI/NoopPageTemplate.swift` | iOS, Android | noop page template |
| `StrandiOS/NoopUI/NoopPermissionPrimer.swift` | iOS, Android | noop permission primer |
| `StrandiOS/NoopUI/NoopPositionCopy.swift` | iOS, Android | noop position copy |
| `StrandiOS/NoopUI/NoopRipple.swift` | iOS, Android | noop ripple |
| `StrandiOS/NoopUI/NoopScheduleInference.swift` | iOS, Android | noop schedule inference |
| `StrandiOS/NoopUI/NoopSessionGuard.swift` | iOS, Android | noop session guard |
| `StrandiOS/NoopUI/NoopShell.swift` | iOS, Android | noop shell |
| `StrandiOS/NoopUI/NoopSlotTiles.swift` | iOS, Android | noop slot tiles |
| `StrandiOS/NoopUI/NoopSystem.swift` | iOS, Android | noop system |
| `StrandiOS/NoopUI/NoopUpdates.swift` | iOS, Android | noop updates |
| `StrandiOS/NoopUI/NoopWidgetLinkCard.swift` | iOS, Android | noop widget link card |

### Onboarding (2)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/NoopUI/NoopOnboarding.swift` | iOS, Android | noop onboarding |
| `StrandiOS/NoopUI/NoopOnboardingScenes.swift` | iOS, Android | noop onboarding scenes |

### Metal shaders (1)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/NoopUI/NoopShaders.metal` | iOS, Android | NoopShaders.metal |

### Aura share cards (7)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/NoopUI/Share/AuraShareCards.swift` | iOS, Android | aura share cards |
| `StrandiOS/NoopUI/Share/AuraShareExport.swift` | iOS, Android | aura share export |
| `StrandiOS/NoopUI/Share/AuraShareModel.swift` | iOS, Android | aura share model |
| `StrandiOS/NoopUI/Share/AuraShareMoment.swift` | iOS, Android | aura share moment |
| `StrandiOS/NoopUI/Share/AuraSharePhotoEditor.swift` | iOS, Android | aura share photo editor |
| `StrandiOS/NoopUI/Share/AuraSharePresenting.swift` | iOS, Android | aura share presenting |
| `StrandiOS/NoopUI/Share/AuraShareSheet.swift` | iOS, Android | aura share sheet |

### iOS resources (icons, Rive, plist) (68)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Aura.appiconset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Aura.appiconset/icon_1024.png` | iOS | image asset: icon_1024 |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Aura.appiconset/icon_1024_dark.png` | iOS | image asset: icon_1024_dark |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Aurora.appiconset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Aurora.appiconset/icon_1024.png` | iOS | image asset: icon_1024 |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Aurora.appiconset/icon_1024_dark.png` | iOS | image asset: icon_1024_dark |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Breathe.appiconset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Breathe.appiconset/icon_1024.png` | iOS | image asset: icon_1024 |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Breathe.appiconset/icon_1024_dark.png` | iOS | image asset: icon_1024_dark |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Ember.appiconset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Ember.appiconset/icon_1024.png` | iOS | image asset: icon_1024 |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Ember.appiconset/icon_1024_dark.png` | iOS | image asset: icon_1024_dark |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Frost.appiconset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Frost.appiconset/icon_1024.png` | iOS | image asset: icon_1024 |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Frost.appiconset/icon_1024_dark.png` | iOS | image asset: icon_1024_dark |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Navy.appiconset/icon_1024_dark.png` | iOS | image asset: icon_1024_dark |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Orb.appiconset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Orb.appiconset/icon_1024.png` | iOS | image asset: icon_1024 |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Orb.appiconset/icon_1024_dark.png` | iOS | image asset: icon_1024_dark |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Titanium.appiconset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Titanium.appiconset/icon_1024.png` | iOS | image asset: icon_1024 |
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Titanium.appiconset/icon_1024_dark.png` | iOS | image asset: icon_1024_dark |
| `StrandiOS/Resources/Assets.xcassets/AppIcon.appiconset/icon_1024_dark.png` | iOS | image asset: icon_1024_dark |
| `StrandiOS/Resources/Assets.xcassets/CafeBlackCoffee.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/CafeBlackCoffee.imageset/black-coffee.jpg` | iOS | black-coffee.jpg |
| `StrandiOS/Resources/Assets.xcassets/CafeCappuccino.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/CafeCappuccino.imageset/cappuccino.jpg` | iOS | cappuccino.jpg |
| `StrandiOS/Resources/Assets.xcassets/CafeDoubleCappuccino.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/CafeDoubleCappuccino.imageset/double-cappuccino.jpg` | iOS | double-cappuccino.jpg |
| `StrandiOS/Resources/Assets.xcassets/CafeMatcha.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/CafeMatcha.imageset/matcha.jpg` | iOS | matcha.jpg |
| `StrandiOS/Resources/Assets.xcassets/CafeSurprise.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/CafeSurprise.imageset/surprise-me.jpg` | iOS | surprise-me.jpg |
| `StrandiOS/Resources/Assets.xcassets/CafeWholeCafe.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/CafeWholeCafe.imageset/whole-cafe.jpg` | iOS | whole-cafe.jpg |
| `StrandiOS/Resources/Assets.xcassets/IconSpooky.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/IconSpooky.imageset/icon.png` | iOS | image asset: icon |
| `StrandiOS/Resources/Assets.xcassets/LaunchBackground.colorset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/NavyTitanium.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/NavyTitanium.imageset/navy-titanium-1024.png` | iOS | image asset: navy-titanium-1024 |
| `StrandiOS/Resources/Assets.xcassets/TwitterBird.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/TwitterBird.imageset/twitter-bird.svg` | iOS | twitter-bird.svg |
| `StrandiOS/Resources/Assets.xcassets/import-garmin.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/import-garmin.imageset/garmin.svg` | iOS | garmin.svg |
| `StrandiOS/Resources/Assets.xcassets/import-google.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/import-google.imageset/google.svg` | iOS | google.svg |
| `StrandiOS/Resources/Assets.xcassets/import-oura.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/import-oura.imageset/oura.svg` | iOS | oura.svg |
| `StrandiOS/Resources/Assets.xcassets/import-polar.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/import-polar.imageset/polar.svg` | iOS | polar.svg |
| `StrandiOS/Resources/Assets.xcassets/import-samsung.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/import-samsung.imageset/samsung.svg` | iOS | samsung.svg |
| `StrandiOS/Resources/Assets.xcassets/import-strava.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/import-strava.imageset/strava.svg` | iOS | strava.svg |
| `StrandiOS/Resources/Assets.xcassets/import-withings.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/import-withings.imageset/withings.svg` | iOS | withings.svg |
| `StrandiOS/Resources/Assets.xcassets/import-xiaomi.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/import-xiaomi.imageset/xiaomi.svg` | iOS | xiaomi.svg |
| `StrandiOS/Resources/Assets.xcassets/import-zepp.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/import-zepp.imageset/zepp.svg` | iOS | zepp.svg |
| `StrandiOS/Resources/Assets.xcassets/marv-avatar.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/marv-avatar.imageset/marv-avatar.png` | iOS | image asset: marv-avatar |
| `StrandiOS/Resources/Assets.xcassets/svea-avatar.imageset/Contents.json` | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/svea-avatar.imageset/svea-avatar.png` | iOS | image asset: svea-avatar |
| `StrandiOS/Resources/Rive/chest_common.riv` | iOS | Rive animation asset |
| `StrandiOS/Resources/Rive/chest_quest.riv` | iOS | Rive animation asset |
| `StrandiOS/Resources/Rive/chest_rare.riv` | iOS | Rive animation asset |
| `StrandiOS/Resources/Rive/gift_waiting.riv` | iOS | Rive animation asset |

### iOS system (intents, sync, streaks) (4)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/System/NoopAutomationIntents.swift` | iOS, Android | noop automation intents |
| `StrandiOS/System/StravaSync.swift` | iOS, Android | strava sync |
| `StrandiOS/System/StreakAlarm.swift` | iOS, Android | streak alarm |
| `StrandiOS/System/StreakCheckBackgroundScheduler.swift` | iOS, Android | streak check background scheduler |

### Live Activities (iOS) (4)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOS/Widgets/NoopCompanionPublish.swift` | iOS, Android | noop companion publish |
| `StrandiOS/Widgets/StreakLiveActivityController.swift` | iOS, Android | streak live activity controller |
| `StrandiOS/Widgets/WidgetBoard.swift` | iOS, Android | widget board |
| `StrandiOS/Widgets/WidgetPanelsPublish.swift` | iOS, Android | widget panels publish |

### iOS shared (widgets/live-activity attrs) (5)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOSShared/LiveSessionControl.swift` | iOS, Android | live session control |
| `StrandiOSShared/StreakActivityAttributes.swift` | iOS, Android | streak activity attributes |
| `StrandiOSShared/WidgetCompanion.swift` | iOS, Android | widget companion |
| `StrandiOSShared/WidgetLink.swift` | iOS, Android | widget link |
| `StrandiOSShared/WidgetPanels.swift` | iOS, Android | widget panels |

### Widgets / Live Activities (iOS) (3)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOSWidgets/NOOPRingsWidget.swift` | iOS, Android | noop rings widget |
| `StrandiOSWidgets/NoopControls.swift` | iOS, Android | noop controls |
| `StrandiOSWidgets/StreakLiveActivity.swift` | iOS, Android | streak live activity |

### Widget panels (9)

| Path | Platforms | Description |
|---|---|---|
| `StrandiOSWidgets/Panels/CompanionWidgets.swift` | iOS, Android | companion widgets |
| `StrandiOSWidgets/Panels/GoalWidget.swift` | iOS, Android | goal widget |
| `StrandiOSWidgets/Panels/LiftBanner.swift` | iOS, Android | lift banner |
| `StrandiOSWidgets/Panels/PanelKit.swift` | iOS, Android | panel kit |
| `StrandiOSWidgets/Panels/SleepWidget.swift` | iOS, Android | sleep widget |
| `StrandiOSWidgets/Panels/StepsWidget.swift` | iOS, Android | steps widget |
| `StrandiOSWidgets/Panels/TrainingWidgets.swift` | iOS, Android | training widgets |
| `StrandiOSWidgets/Panels/VitalsWidget.swift` | iOS, Android | vitals widget |
| `StrandiOSWidgets/Panels/WeightWidget.swift` | iOS, Android | weight widget |

### On-device LLM (llama.cpp + Gemma) (4)

| Path | Platforms | Description |
|---|---|---|
| `Vendor/Nomic/Resources/.gitkeep` | iOS, Android | .gitkeep |
| `Vendor/Nomic/Resources/Gemma-NOTICE.txt` | iOS, Android | Gemma model notice |
| `Vendor/Nomic/Resources/Gemma-Prohibited-Use-Policy.txt` | iOS, Android | Gemma prohibited-use policy |
| `Vendor/Nomic/Resources/Gemma-Terms.txt` | iOS, Android | Gemma model terms |

---

## 2. Modified files (648)

Direction key: `aura-lead` = Aura has substantial content the repo lacks (port, preserving repo-ahead lines); `merge (both sides)` = both sides changed substantially (true merge); `repo-lead (verify only)` = repo is newer — keep repo content, verify no Aura-specific feature is missing; `minor` = small delta.

### Build config (xcconfig) (2)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Config/BundleId.xcconfig` | repo-lead (verify only) | iOS, macOS | BundleId.xcconfig |
| `Config/BundleIdSecrets.example.xcconfig` | minor | iOS, macOS | BundleIdSecrets.example.xcconfig |

### Licensing / docs (4)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `LICENSE` | repo-lead (verify only) | iOS, macOS, Android | license |
| `NOTICE` | repo-lead (verify only) | iOS, macOS, Android | notice |
| `README.md` | repo-lead (verify only) | iOS, macOS, Android | README.md |
| `TERMS.md` | aura-lead | iOS, macOS, Android | TERMS.md |

### Local access / data export (3)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Packages/NoopLocalAccess/Package.swift` | repo-lead (verify only) | iOS, macOS, Android | package |
| `Packages/NoopLocalAccess/Sources/NoopLocalAccessCore/LocalAccessCore.swift` | repo-lead (verify only) | iOS, macOS, Android | local access core |
| `Packages/NoopLocalAccess/Sources/NoopLocalAccessCore/ToolDispatcher.swift` | minor | iOS, macOS, Android | tool dispatcher |

### Other wearables (Oura/Polar) (22)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Packages/OuraProtocol/Package.swift` | minor | iOS, macOS, Android | package |
| `Packages/OuraProtocol/Sources/OuraProtocol/ActivityEstimate.swift` | repo-lead (verify only) | iOS, macOS, Android | activity estimate |
| `Packages/OuraProtocol/Sources/OuraProtocol/Auth.swift` | merge (both sides) | iOS, macOS, Android | auth |
| `Packages/OuraProtocol/Sources/OuraProtocol/Commands.swift` | repo-lead (verify only) | iOS, macOS, Android | commands |
| `Packages/OuraProtocol/Sources/OuraProtocol/Decoders.swift` | merge (both sides) | iOS, macOS, Android | decoders |
| `Packages/OuraProtocol/Sources/OuraProtocol/EventTags.swift` | merge (both sides) | iOS, macOS, Android | event tags |
| `Packages/OuraProtocol/Sources/OuraProtocol/Framing.swift` | repo-lead (verify only) | iOS, macOS, Android | framing |
| `Packages/OuraProtocol/Sources/OuraProtocol/HypnogramAssembler.swift` | repo-lead (verify only) | iOS, macOS, Android | hypnogram assembler |
| `Packages/OuraProtocol/Sources/OuraProtocol/OuraAuthWatchdog.swift` | repo-lead (verify only) | iOS, macOS, Android | oura auth watchdog |
| `Packages/OuraProtocol/Sources/OuraProtocol/OuraDriver.swift` | merge (both sides) | iOS, macOS, Android | oura driver |
| `Packages/OuraProtocol/Sources/OuraProtocol/OuraEvents.swift` | merge (both sides) | iOS, macOS, Android | oura events |
| `Packages/OuraProtocol/Sources/OuraProtocol/OuraGatt.swift` | repo-lead (verify only) | iOS, macOS, Android | oura gatt |
| `Packages/OuraProtocol/Sources/OuraProtocol/OuraHistoryDrain.swift` | repo-lead (verify only) | iOS, macOS, Android | oura history drain |
| `Packages/OuraProtocol/Sources/OuraProtocol/OuraIbiHr.swift` | repo-lead (verify only) | iOS, macOS, Android | oura ibi hr |
| `Packages/OuraProtocol/Sources/OuraProtocol/OuraWear.swift` | repo-lead (verify only) | iOS, macOS, Android | oura wear |
| `Packages/OuraProtocol/Sources/OuraProtocol/RingGen.swift` | repo-lead (verify only) | iOS, macOS, Android | ring gen |
| `Packages/OuraProtocol/Sources/OuraProtocol/UserInfo.swift` | repo-lead (verify only) | iOS, macOS, Android | user info |
| `Packages/OuraProtocol/Sources/oura-decode/main.swift` | repo-lead (verify only) | iOS, macOS, Android | main |
| `Packages/PolarProtocol/Package.swift` | minor | iOS, macOS, Android | package |
| `Packages/PolarProtocol/Sources/PolarProtocol/PmdControl.swift` | repo-lead (verify only) | iOS, macOS, Android | pmd control |
| `Packages/PolarProtocol/Sources/PolarProtocol/PmdDecoder.swift` | merge (both sides) | iOS, macOS, Android | pmd decoder |
| `Packages/PolarProtocol/Sources/PolarProtocol/PolarModel.swift` | repo-lead (verify only) | iOS, macOS, Android | polar model |

### Analytics math (124)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Packages/StrandAnalytics/Package.resolved` | minor | iOS, macOS, Android | Package.resolved |
| `Packages/StrandAnalytics/Package.swift` | minor | iOS, macOS, Android | package |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ActivityCostEngine.swift` | merge (both sides) | iOS, macOS, Android | activity cost engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ActivityHeatmap.swift` | repo-lead (verify only) | iOS, macOS, Android | activity heatmap |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/AdaptiveExpenditureEngine.swift` | repo-lead (verify only) | iOS, macOS, Android | adaptive expenditure engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/AlarmReadback.swift` | repo-lead (verify only) | iOS, macOS, Android | alarm readback |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/AnalysisPhaseTally.swift` | repo-lead (verify only) | iOS, macOS, Android | analysis phase tally |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/AnalyticsEngine.swift` | merge (both sides) | iOS, macOS, Android | analytics engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/AnalyticsMemo.swift` | repo-lead (verify only) | iOS, macOS, Android | analytics memo |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/AnalyzeRecentDayCache.swift` | repo-lead (verify only) | iOS, macOS, Android | analyze recent day cache |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/AutoWorkoutDetector+Trace.swift` | merge (both sides) | iOS, macOS, Android | auto workout detector+trace |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/AutoWorkoutDetector.swift` | merge (both sides) | iOS, macOS, Android | auto workout detector |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/Baselines+Trace.swift` | repo-lead (verify only) | iOS, macOS, Android | baselines+trace |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/Baselines.swift` | merge (both sides) | iOS, macOS, Android | baselines |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/BatteryEstimator.swift` | merge (both sides) | iOS, macOS, Android | battery estimator |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/BehaviorInsights.swift` | merge (both sides) | iOS, macOS, Android | behavior insights |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/BreathPacer.swift` | merge (both sides) | iOS, macOS, Android | breath pacer |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/BreathProtocol.swift` | merge (both sides) | iOS, macOS, Android | breath protocol |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/BreathProtocolCatalog.swift` | repo-lead (verify only) | iOS, macOS, Android | breath protocol catalog |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/CaptureAccumulator.swift` | repo-lead (verify only) | iOS, macOS, Android | capture accumulator |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/CaptureCompleteness.swift` | repo-lead (verify only) | iOS, macOS, Android | capture completeness |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ChargeBaselines.swift` | repo-lead (verify only) | iOS, macOS, Android | charge baselines |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ChargeDrivers.swift` | merge (both sides) | iOS, macOS, Android | charge drivers |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/CircadianEngine.swift` | merge (both sides) | iOS, macOS, Android | circadian engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ClockFormat.swift` | repo-lead (verify only) | iOS, macOS, Android | clock format |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/CoachSuggestions.swift` | repo-lead (verify only) | iOS, macOS, Android | coach suggestions |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ComparisonEngine.swift` | merge (both sides) | iOS, macOS, Android | comparison engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ConnectionReadout.swift` | repo-lead (verify only) | iOS, macOS, Android | connection readout |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/CorrelationEngine.swift` | repo-lead (verify only) | iOS, macOS, Android | correlation engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/CyclePhaseEngine.swift` | merge (both sides) | iOS, macOS, Android | cycle phase engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/DayCycle.swift` | aura-lead | iOS, macOS, Android | day cycle |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/DayOwnerResolver.swift` | minor | iOS, macOS, Android | day owner resolver |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/DaytimeBaselines.swift` | repo-lead (verify only) | iOS, macOS, Android | daytime baselines |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/DaytimeStress.swift` | merge (both sides) | iOS, macOS, Android | daytime stress |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/DisplayTrace.swift` | merge (both sides) | iOS, macOS, Android | display trace |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/DoseResponseEngine.swift` | merge (both sides) | iOS, macOS, Android | dose response engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/DoseResponsePriors.swift` | repo-lead (verify only) | iOS, macOS, Android | dose response priors |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/EffectRanker.swift` | aura-lead | iOS, macOS, Android | effect ranker |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/FitnessAgeEngine.swift` | merge (both sides) | iOS, macOS, Android | fitness age engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/FusionResolver.swift` | repo-lead (verify only) | iOS, macOS, Android | fusion resolver |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/FusionTypes.swift` | merge (both sides) | iOS, macOS, Android | fusion types |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/GuidedCaptureProgress.swift` | minor | iOS, macOS, Android | guided capture progress |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/HRDownPacer.swift` | merge (both sides) | iOS, macOS, Android | hr down pacer |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/HRVAnalyzer+Trace.swift` | repo-lead (verify only) | iOS, macOS, Android | hrv analyzer+trace |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/HRVAnalyzer.swift` | merge (both sides) | iOS, macOS, Android | hrv analyzer |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/HRVFreqDomain.swift` | merge (both sides) | iOS, macOS, Android | hrv freq domain |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/HRVReadiness.swift` | merge (both sides) | iOS, macOS, Android | hrv readiness |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/HRZones.swift` | aura-lead | iOS, macOS, Android | hr zones |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/HeartRateRecovery.swift` | repo-lead (verify only) | iOS, macOS, Android | heart rate recovery |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/HydrationGoal.swift` | repo-lead (verify only) | iOS, macOS, Android | hydration goal |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/IllnessDistance.swift` | repo-lead (verify only) | iOS, macOS, Android | illness distance |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/IllnessSignalEngine.swift` | merge (both sides) | iOS, macOS, Android | illness signal engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ImuFeatureExtractor.swift` | repo-lead (verify only) | iOS, macOS, Android | imu feature extractor |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/LabBookProjection.swift` | merge (both sides) | iOS, macOS, Android | lab book projection |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/LiftMetrics.swift` | repo-lead (verify only) | iOS, macOS, Android | lift metrics |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/LivePersistTrace.swift` | repo-lead (verify only) | iOS, macOS, Android | live persist trace |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/LiveSessionEngine.swift` | merge (both sides) | iOS, macOS, Android | live session engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/LocalDayWindows.swift` | repo-lead (verify only) | iOS, macOS, Android | local day windows |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ManualWorkoutRescore.swift` | repo-lead (verify only) | iOS, macOS, Android | manual workout rescore |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/MetricArbitrationPolicy.swift` | merge (both sides) | iOS, macOS, Android | metric arbitration policy |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/NightStandDown.swift` | repo-lead (verify only) | iOS, macOS, Android | night stand down |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/PhysiologicalSteps.swift` | minor | iOS, macOS, Android | physiological steps |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/PreSleepHeartRateFeedback.swift` | repo-lead (verify only) | iOS, macOS, Android | pre sleep heart rate feedback |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/PrimarySessionRestingHR.swift` | repo-lead (verify only) | iOS, macOS, Android | primary session resting hr |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/RangeReport.swift` | merge (both sides) | iOS, macOS, Android | range report |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ReadinessEngine.swift` | merge (both sides) | iOS, macOS, Android | readiness engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ReadinessTrainingLoad.swift` | repo-lead (verify only) | iOS, macOS, Android | readiness training load |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/RecoveryForecast.swift` | merge (both sides) | iOS, macOS, Android | recovery forecast |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/RecoveryScorer+Trace.swift` | repo-lead (verify only) | iOS, macOS, Android | recovery scorer+trace |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/RecoveryScorer.swift` | merge (both sides) | iOS, macOS, Android | recovery scorer |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ResonanceEngine.swift` | merge (both sides) | iOS, macOS, Android | resonance engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/RestSubScoreTrace.swift` | merge (both sides) | iOS, macOS, Android | rest sub score trace |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/RhythmExport.swift` | repo-lead (verify only) | iOS, macOS, Android | rhythm export |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/RhythmScreener.swift` | merge (both sides) | iOS, macOS, Android | rhythm screener |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/RrEmissionStats.swift` | merge (both sides) | iOS, macOS, Android | rr emission stats |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/ScoreConfidence.swift` | repo-lead (verify only) | iOS, macOS, Android | score confidence |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SedentaryDetector.swift` | merge (both sides) | iOS, macOS, Android | sedentary detector |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SkinTempBackfill.swift` | merge (both sides) | iOS, macOS, Android | skin temp backfill |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SkinTempDisplay.swift` | repo-lead (verify only) | iOS, macOS, Android | skin temp display |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepAwareStepCounter.swift` | minor | iOS, macOS, Android | sleep aware step counter |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepDebt.swift` | merge (both sides) | iOS, macOS, Android | sleep debt |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepEditGuard.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep edit guard |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepHeartRateContrast.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep heart rate contrast |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepNightLabel.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep night label |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepReadout.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep readout |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepSkipSummary.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep skip summary |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepStageTotals.swift` | merge (both sides) | iOS, macOS, Android | sleep stage totals |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepStageVocabulary.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep stage vocabulary |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepStager+Trace.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep stager+trace |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepStager.swift` | merge (both sides) | iOS, macOS, Android | sleep stager |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepStagerV2.swift` | merge (both sides) | iOS, macOS, Android | sleep stager v2 |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SleepWindowReclip.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep window reclip |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SlidingStreamWindow.swift` | repo-lead (verify only) | iOS, macOS, Android | sliding stream window |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/Spo2ReTrace.swift` | repo-lead (verify only) | iOS, macOS, Android | spo2 re trace |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SpotHrvReading.swift` | repo-lead (verify only) | iOS, macOS, Android | spot hrv reading |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/SseDeltas.swift` | repo-lead (verify only) | iOS, macOS, Android | sse deltas |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StagePercentages.swift` | repo-lead (verify only) | iOS, macOS, Android | stage percentages |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StepsCounter.swift` | repo-lead (verify only) | iOS, macOS, Android | steps counter |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StepsDetailDensity.swift` | repo-lead (verify only) | iOS, macOS, Android | steps detail density |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StepsEstimateEngine+Trace.swift` | repo-lead (verify only) | iOS, macOS, Android | steps estimate engine+trace |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StepsEstimateEngine.swift` | merge (both sides) | iOS, macOS, Android | steps estimate engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StepsMotionCache.swift` | repo-lead (verify only) | iOS, macOS, Android | steps motion cache |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StoreProbeTally.swift` | repo-lead (verify only) | iOS, macOS, Android | store probe tally |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StrainScorer.swift` | merge (both sides) | iOS, macOS, Android | strain scorer |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StrandAnalytics.swift` | repo-lead (verify only) | iOS, macOS, Android | strand analytics |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StrapComparison.swift` | repo-lead (verify only) | iOS, macOS, Android | strap comparison |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StreakCalculator.swift` | repo-lead (verify only) | iOS, macOS, Android | streak calculator |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StreamReadCap.swift` | repo-lead (verify only) | iOS, macOS, Android | stream read cap |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StressIndex.swift` | repo-lead (verify only) | iOS, macOS, Android | stress index |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/StressOnsetDetector.swift` | merge (both sides) | iOS, macOS, Android | stress onset detector |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/TestCentreLayout.swift` | repo-lead (verify only) | iOS, macOS, Android | test centre layout |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/TestDomain.swift` | merge (both sides) | iOS, macOS, Android | test domain |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/TestModeRegistry.swift` | merge (both sides) | iOS, macOS, Android | test mode registry |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/TrainingLoadEngine.swift` | repo-lead (verify only) | iOS, macOS, Android | training load engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/UniversalTrace.swift` | repo-lead (verify only) | iOS, macOS, Android | universal trace |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/VitalBands.swift` | merge (both sides) | iOS, macOS, Android | vital bands |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/VitalityEngine.swift` | merge (both sides) | iOS, macOS, Android | vitality engine |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/WakeMotionRefinement.swift` | repo-lead (verify only) | iOS, macOS, Android | wake motion refinement |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/WatchRecovery.swift` | repo-lead (verify only) | iOS, macOS, Android | watch recovery |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/WeeklyDigest.swift` | merge (both sides) | iOS, macOS, Android | weekly digest |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/WindowedStreamPlan.swift` | repo-lead (verify only) | iOS, macOS, Android | windowed stream plan |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/WorkoutDetector.swift` | merge (both sides) | iOS, macOS, Android | workout detector |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/WorkoutTypeClassifier.swift` | merge (both sides) | iOS, macOS, Android | workout type classifier |
| `Packages/StrandAnalytics/Sources/StrandAnalytics/WristWearRecovery.swift` | minor | iOS, macOS, Android | wrist wear recovery |

### Design system (tokens, charts, icons) (35)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Packages/StrandDesign/Package.swift` | minor | iOS, macOS, Android | package |
| `Packages/StrandDesign/Sources/StrandDesign/Appearance.swift` | merge (both sides) | iOS, macOS, Android | appearance |
| `Packages/StrandDesign/Sources/StrandDesign/BrandMark.swift` | repo-lead (verify only) | iOS, macOS, Android | brand mark |
| `Packages/StrandDesign/Sources/StrandDesign/ChargeSyncIndicator.swift` | merge (both sides) | iOS, macOS, Android | charge sync indicator |
| `Packages/StrandDesign/Sources/StrandDesign/ChartAxisDays.swift` | repo-lead (verify only) | iOS, macOS, Android | chart axis days |
| `Packages/StrandDesign/Sources/StrandDesign/ChartHover.swift` | repo-lead (verify only) | iOS, macOS, Android | chart hover |
| `Packages/StrandDesign/Sources/StrandDesign/Components.swift` | merge (both sides) | iOS, macOS, Android | components |
| `Packages/StrandDesign/Sources/StrandDesign/DayNavBar.swift` | repo-lead (verify only) | iOS, macOS, Android | day nav bar |
| `Packages/StrandDesign/Sources/StrandDesign/DomainTheme.swift` | merge (both sides) | iOS, macOS, Android | domain theme |
| `Packages/StrandDesign/Sources/StrandDesign/GlowRing.swift` | repo-lead (verify only) | iOS, macOS, Android | glow ring |
| `Packages/StrandDesign/Sources/StrandDesign/Haptics.swift` | repo-lead (verify only) | iOS, macOS, Android | haptics |
| `Packages/StrandDesign/Sources/StrandDesign/Hypnogram.swift` | merge (both sides) | iOS, macOS, Android | hypnogram |
| `Packages/StrandDesign/Sources/StrandDesign/Motion.swift` | repo-lead (verify only) | iOS, macOS, Android | motion |
| `Packages/StrandDesign/Sources/StrandDesign/MotionTrace.swift` | repo-lead (verify only) | iOS, macOS, Android | motion trace |
| `Packages/StrandDesign/Sources/StrandDesign/NoopButton.swift` | merge (both sides) | iOS, macOS, Android | noop button |
| `Packages/StrandDesign/Sources/StrandDesign/NoopLiquidGlassSearchField.swift` | merge (both sides) | iOS, macOS, Android | noop liquid glass search field |
| `Packages/StrandDesign/Sources/StrandDesign/NoopMotion.swift` | merge (both sides) | iOS, macOS, Android | noop motion |
| `Packages/StrandDesign/Sources/StrandDesign/NoopVisualStyle.swift` | repo-lead (verify only) | iOS, macOS, Android | noop visual style |
| `Packages/StrandDesign/Sources/StrandDesign/OverviewHRChart.swift` | merge (both sides) | iOS, macOS, Android | overview hr chart |
| `Packages/StrandDesign/Sources/StrandDesign/Palette.swift` | aura-lead | iOS, macOS, Android | palette |
| `Packages/StrandDesign/Sources/StrandDesign/PipBar.swift` | merge (both sides) | iOS, macOS, Android | pip bar |
| `Packages/StrandDesign/Sources/StrandDesign/RecoveryRing.swift` | merge (both sides) | iOS, macOS, Android | recovery ring |
| `Packages/StrandDesign/Sources/StrandDesign/SceneHeroBackground.swift` | merge (both sides) | iOS, macOS, Android | scene hero background |
| `Packages/StrandDesign/Sources/StrandDesign/Sparkline.swift` | merge (both sides) | iOS, macOS, Android | sparkline |
| `Packages/StrandDesign/Sources/StrandDesign/SportIcon.swift` | aura-lead | iOS, macOS, Android | sport icon |
| `Packages/StrandDesign/Sources/StrandDesign/StatePill.swift` | repo-lead (verify only) | iOS, macOS, Android | state pill |
| `Packages/StrandDesign/Sources/StrandDesign/StrainGauge.swift` | repo-lead (verify only) | iOS, macOS, Android | strain gauge |
| `Packages/StrandDesign/Sources/StrandDesign/StrandCard.swift` | merge (both sides) | iOS, macOS, Android | strand card |
| `Packages/StrandDesign/Sources/StrandDesign/StrandDesign.swift` | repo-lead (verify only) | iOS, macOS, Android | strand design |
| `Packages/StrandDesign/Sources/StrandDesign/TimeOfDayBackground.swift` | merge (both sides) | iOS, macOS, Android | time of day background |
| `Packages/StrandDesign/Sources/StrandDesign/TrendChart.swift` | merge (both sides) | iOS, macOS, Android | trend chart |
| `Packages/StrandDesign/Sources/StrandDesign/TypicalRangeBar.swift` | merge (both sides) | iOS, macOS, Android | typical range bar |
| `Packages/StrandDesign/Sources/StrandDesign/Typography.swift` | merge (both sides) | iOS, macOS, Android | typography |
| `Packages/StrandDesign/Sources/StrandDesign/WatchScoreSnapshot.swift` | repo-lead (verify only) | iOS, macOS, Android | watch score snapshot |
| `Packages/StrandDesign/Sources/StrandDesign/YearHeatStrip.swift` | merge (both sides) | iOS, macOS, Android | year heat strip |

### Importers (Hevy, Strava, Mi, Google, Health) (39)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Packages/StrandImport/Package.swift` | repo-lead (verify only) | iOS, macOS, Android | package |
| `Packages/StrandImport/Sources/StrandImport/ActivityFileImporter.swift` | merge (both sides) | iOS, macOS, Android | activity file importer |
| `Packages/StrandImport/Sources/StrandImport/AppleHealthAggregator.swift` | merge (both sides) | iOS, macOS, Android | apple health aggregator |
| `Packages/StrandImport/Sources/StrandImport/AppleHealthImporter.swift` | merge (both sides) | iOS, macOS, Android | apple health importer |
| `Packages/StrandImport/Sources/StrandImport/CSVParsing.swift` | merge (both sides) | iOS, macOS, Android | csv parsing |
| `Packages/StrandImport/Sources/StrandImport/CorelibsCompat.swift` | repo-lead (verify only) | iOS, macOS, Android | corelibs compat |
| `Packages/StrandImport/Sources/StrandImport/FitParser.swift` | merge (both sides) | iOS, macOS, Android | fit parser |
| `Packages/StrandImport/Sources/StrandImport/FitbitExportParser.swift` | repo-lead (verify only) | iOS, macOS, Android | fitbit export parser |
| `Packages/StrandImport/Sources/StrandImport/GarminExportParser.swift` | repo-lead (verify only) | iOS, macOS, Android | garmin export parser |
| `Packages/StrandImport/Sources/StrandImport/GpxParser.swift` | repo-lead (verify only) | iOS, macOS, Android | gpx parser |
| `Packages/StrandImport/Sources/StrandImport/HealthWriteback.swift` | aura-lead | iOS, macOS, Android | health writeback |
| `Packages/StrandImport/Sources/StrandImport/HevyAPI.swift` | aura-lead | iOS, macOS, Android | hevy api |
| `Packages/StrandImport/Sources/StrandImport/ImportColumnCoverage.swift` | repo-lead (verify only) | iOS, macOS, Android | import column coverage |
| `Packages/StrandImport/Sources/StrandImport/ImportCoordinator.swift` | repo-lead (verify only) | iOS, macOS, Android | import coordinator |
| `Packages/StrandImport/Sources/StrandImport/ImportModels.swift` | merge (both sides) | iOS, macOS, Android | import models |
| `Packages/StrandImport/Sources/StrandImport/ImportTrace.swift` | repo-lead (verify only) | iOS, macOS, Android | import trace |
| `Packages/StrandImport/Sources/StrandImport/LabMarker.swift` | repo-lead (verify only) | iOS, macOS, Android | lab marker |
| `Packages/StrandImport/Sources/StrandImport/LabMarkerCsvImport.swift` | merge (both sides) | iOS, macOS, Android | lab marker csv import |
| `Packages/StrandImport/Sources/StrandImport/LiftProgramSheetImporter.swift` | merge (both sides) | iOS, macOS, Android | lift program sheet importer |
| `Packages/StrandImport/Sources/StrandImport/LiftingImporter.swift` | merge (both sides) | iOS, macOS, Android | lifting importer |
| `Packages/StrandImport/Sources/StrandImport/MarkerCatalog.swift` | merge (both sides) | iOS, macOS, Android | marker catalog |
| `Packages/StrandImport/Sources/StrandImport/MuscleGroups.swift` | repo-lead (verify only) | iOS, macOS, Android | muscle groups |
| `Packages/StrandImport/Sources/StrandImport/NutritionCsvImport.swift` | repo-lead (verify only) | iOS, macOS, Android | nutrition csv import |
| `Packages/StrandImport/Sources/StrandImport/OuraApiModels.swift` | minor | iOS, macOS, Android | oura api models |
| `Packages/StrandImport/Sources/StrandImport/OuraApiParser+Daily.swift` | minor | iOS, macOS, Android | oura api parser+daily |
| `Packages/StrandImport/Sources/StrandImport/OuraApiParser+Events.swift` | minor | iOS, macOS, Android | oura api parser+events |
| `Packages/StrandImport/Sources/StrandImport/OuraApiParser.swift` | repo-lead (verify only) | iOS, macOS, Android | oura api parser |
| `Packages/StrandImport/Sources/StrandImport/OuraExportParser.swift` | merge (both sides) | iOS, macOS, Android | oura export parser |
| `Packages/StrandImport/Sources/StrandImport/OuraHypnogram.swift` | repo-lead (verify only) | iOS, macOS, Android | oura hypnogram |
| `Packages/StrandImport/Sources/StrandImport/RouteExporter.swift` | repo-lead (verify only) | iOS, macOS, Android | route exporter |
| `Packages/StrandImport/Sources/StrandImport/StrandImport.swift` | minor | iOS, macOS, Android | strand import |
| `Packages/StrandImport/Sources/StrandImport/TcxParser.swift` | repo-lead (verify only) | iOS, macOS, Android | tcx parser |
| `Packages/StrandImport/Sources/StrandImport/WearableExportImporter.swift` | merge (both sides) | iOS, macOS, Android | wearable export importer |
| `Packages/StrandImport/Sources/StrandImport/WhoopBiomarkerExportParser.swift` | repo-lead (verify only) | iOS, macOS, Android | whoop biomarker export parser |
| `Packages/StrandImport/Sources/StrandImport/WhoopCsvExporter.swift` | merge (both sides) | iOS, macOS, Android | whoop csv exporter |
| `Packages/StrandImport/Sources/StrandImport/WhoopDayKeying.swift` | repo-lead (verify only) | iOS, macOS, Android | whoop day keying |
| `Packages/StrandImport/Sources/StrandImport/WhoopExportImporter.swift` | merge (both sides) | iOS, macOS, Android | whoop export importer |
| `Packages/StrandImport/Sources/StrandImport/XiaomiBandImporter.swift` | merge (both sides) | iOS, macOS, Android | xiaomi band importer |
| `Packages/StrandImport/Sources/StrandImport/XlsxSheet.swift` | repo-lead (verify only) | iOS, macOS, Android | xlsx sheet |

### WHOOP protocol (45)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Packages/WhoopProtocol/Package.swift` | repo-lead (verify only) | iOS, macOS, Android | package |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/BadClockDiagnostics.swift` | repo-lead (verify only) | iOS, macOS, Android | bad clock diagnostics |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/BatteryPackInfo.swift` | repo-lead (verify only) | iOS, macOS, Android | battery pack info |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/BodyLocationProbe.swift` | repo-lead (verify only) | iOS, macOS, Android | body location probe |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/ChunkClockDiag.swift` | repo-lead (verify only) | iOS, macOS, Android | chunk clock diag |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/DataRange.swift` | repo-lead (verify only) | iOS, macOS, Android | data range |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/DeviceConfigReadProbe.swift` | merge (both sides) | iOS, macOS, Android | device config read probe |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/DeviceConfigWriteGate.swift` | merge (both sides) | iOS, macOS, Android | device config write gate |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/DeviceFamily.swift` | repo-lead (verify only) | iOS, macOS, Android | device family |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/ExtendedBatteryProbe.swift` | repo-lead (verify only) | iOS, macOS, Android | extended battery probe |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/FTMSDecode.swift` | merge (both sides) | iOS, macOS, Android | ftms decode |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/FeatureFlagProbe.swift` | merge (both sides) | iOS, macOS, Android | feature flag probe |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/FirmwareGateDiagnostic.swift` | repo-lead (verify only) | iOS, macOS, Android | firmware gate diagnostic |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/FitnessSensorDecode.swift` | merge (both sides) | iOS, macOS, Android | fitness sensor decode |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/FrameDiagnostics.swift` | repo-lead (verify only) | iOS, macOS, Android | frame diagnostics |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Framing.swift` | merge (both sides) | iOS, macOS, Android | framing |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/HapticClock.swift` | repo-lead (verify only) | iOS, macOS, Android | haptic clock |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/HapticPayloads.swift` | repo-lead (verify only) | iOS, macOS, Android | haptic payloads |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/HelloIdentityProbe.swift` | repo-lead (verify only) | iOS, macOS, Android | hello identity probe |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Hex.swift` | repo-lead (verify only) | iOS, macOS, Android | hex |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/HistoricalLayoutSupport.swift` | repo-lead (verify only) | iOS, macOS, Android | historical layout support |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/HistoricalMeta.swift` | repo-lead (verify only) | iOS, macOS, Android | historical meta |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/HistoricalStreams.swift` | merge (both sides) | iOS, macOS, Android | historical streams |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Interpreter.swift` | merge (both sides) | iOS, macOS, Android | interpreter |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/LiveSessionHaptics.swift` | repo-lead (verify only) | iOS, macOS, Android | live session haptics |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/OpticalExperimentAnalysis.swift` | repo-lead (verify only) | iOS, macOS, Android | optical experiment analysis |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/OuraRedrainCollapse.swift` | repo-lead (verify only) | iOS, macOS, Android | oura redrain collapse |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/PostHooks.swift` | merge (both sides) | iOS, macOS, Android | post hooks |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/PpgHr.swift` | merge (both sides) | iOS, macOS, Android | ppg hr |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/PuffinCapture.swift` | merge (both sides) | iOS, macOS, Android | puffin capture |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/R22Disable.swift` | merge (both sides) | iOS, macOS, Android | r22 disable |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/ReTools.swift` | merge (both sides) | iOS, macOS, Android | re tools |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/ScanAdvertisementSummary.swift` | repo-lead (verify only) | iOS, macOS, Android | scan advertisement summary |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Schema.swift` | repo-lead (verify only) | iOS, macOS, Android | schema |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Streams.swift` | merge (both sides) | iOS, macOS, Android | streams |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Values.swift` | minor | iOS, macOS, Android | values |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/VersionCheck.swift` | minor | iOS, macOS, Android | version check |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Whoop4HelloSerial.swift` | repo-lead (verify only) | iOS, macOS, Android | whoop4 hello serial |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Whoop5Config.swift` | repo-lead (verify only) | iOS, macOS, Android | whoop5 config |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Whoop5Ecg.swift` | merge (both sides) | iOS, macOS, Android | whoop5 ecg |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Whoop5EcgProbe.swift` | merge (both sides) | iOS, macOS, Android | whoop5 ecg probe |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Whoop5RR.swift` | repo-lead (verify only) | iOS, macOS, Android | whoop5 rr |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Whoop5RawImu.swift` | repo-lead (verify only) | iOS, macOS, Android | whoop5 raw imu |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Whoop5RawOptical.swift` | merge (both sides) | iOS, macOS, Android | whoop5 raw optical |
| `Packages/WhoopProtocol/Sources/WhoopProtocol/Whoop5Variant.swift` | repo-lead (verify only) | iOS, macOS, Android | whoop5 variant |

### Storage (WhoopStore/GRDB) (47)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Packages/WhoopStore/Package.swift` | repo-lead (verify only) | iOS, macOS, Android | package |
| `Packages/WhoopStore/Sources/WhoopStore/AppleStepHourStore.swift` | minor | iOS, macOS, Android | apple step hour store |
| `Packages/WhoopStore/Sources/WhoopStore/BackupProvenance.swift` | repo-lead (verify only) | iOS, macOS, Android | backup provenance |
| `Packages/WhoopStore/Sources/WhoopStore/BackupSettings.swift` | merge (both sides) | iOS, macOS, Android | backup settings |
| `Packages/WhoopStore/Sources/WhoopStore/ClockRef.swift` | repo-lead (verify only) | iOS, macOS, Android | clock ref |
| `Packages/WhoopStore/Sources/WhoopStore/CoachMessageStore.swift` | repo-lead (verify only) | iOS, macOS, Android | coach message store |
| `Packages/WhoopStore/Sources/WhoopStore/Cursors.swift` | aura-lead | iOS, macOS, Android | cursors |
| `Packages/WhoopStore/Sources/WhoopStore/Database.swift` | merge (both sides) | iOS, macOS, Android | database |
| `Packages/WhoopStore/Sources/WhoopStore/DatabaseIntegrity.swift` | repo-lead (verify only) | iOS, macOS, Android | database integrity |
| `Packages/WhoopStore/Sources/WhoopStore/DeviceBrandCatalog.swift` | repo-lead (verify only) | iOS, macOS, Android | device brand catalog |
| `Packages/WhoopStore/Sources/WhoopStore/DeviceRegistryStore.swift` | merge (both sides) | iOS, macOS, Android | device registry store |
| `Packages/WhoopStore/Sources/WhoopStore/DismissedSleepSpans.swift` | repo-lead (verify only) | iOS, macOS, Android | dismissed sleep spans |
| `Packages/WhoopStore/Sources/WhoopStore/HypnogramCoverage.swift` | repo-lead (verify only) | iOS, macOS, Android | hypnogram coverage |
| `Packages/WhoopStore/Sources/WhoopStore/JournalWorkoutAppleCache.swift` | merge (both sides) | iOS, macOS, Android | journal workout apple cache |
| `Packages/WhoopStore/Sources/WhoopStore/LabMarkerStore.swift` | repo-lead (verify only) | iOS, macOS, Android | lab marker store |
| `Packages/WhoopStore/Sources/WhoopStore/LiftLogStore.swift` | merge (both sides) | iOS, macOS, Android | lift log store |
| `Packages/WhoopStore/Sources/WhoopStore/LiftMuscle.swift` | repo-lead (verify only) | iOS, macOS, Android | lift muscle |
| `Packages/WhoopStore/Sources/WhoopStore/LiveSessionStore.swift` | repo-lead (verify only) | iOS, macOS, Android | live session store |
| `Packages/WhoopStore/Sources/WhoopStore/MetricSeriesStore.swift` | aura-lead | iOS, macOS, Android | metric series store |
| `Packages/WhoopStore/Sources/WhoopStore/MetricsCache.swift` | merge (both sides) | iOS, macOS, Android | metrics cache |
| `Packages/WhoopStore/Sources/WhoopStore/OuraActivityDumpLine.swift` | repo-lead (verify only) | iOS, macOS, Android | oura activity dump line |
| `Packages/WhoopStore/Sources/WhoopStore/OuraMotionDumpLine.swift` | repo-lead (verify only) | iOS, macOS, Android | oura motion dump line |
| `Packages/WhoopStore/Sources/WhoopStore/OuraRawDumpLine.swift` | repo-lead (verify only) | iOS, macOS, Android | oura raw dump line |
| `Packages/WhoopStore/Sources/WhoopStore/OuraRawStore.swift` | repo-lead (verify only) | iOS, macOS, Android | oura raw store |
| `Packages/WhoopStore/Sources/WhoopStore/OuraRealStepsDumpLine.swift` | repo-lead (verify only) | iOS, macOS, Android | oura real steps dump line |
| `Packages/WhoopStore/Sources/WhoopStore/OuraRespScale.swift` | repo-lead (verify only) | iOS, macOS, Android | oura resp scale |
| `Packages/WhoopStore/Sources/WhoopStore/OuraRingEpochScan.swift` | repo-lead (verify only) | iOS, macOS, Android | oura ring epoch scan |
| `Packages/WhoopStore/Sources/WhoopStore/OuraSerialIdentity.swift` | repo-lead (verify only) | iOS, macOS, Android | oura serial identity |
| `Packages/WhoopStore/Sources/WhoopStore/OuraSidecarGenerations.swift` | repo-lead (verify only) | iOS, macOS, Android | oura sidecar generations |
| `Packages/WhoopStore/Sources/WhoopStore/OuraSleepSessionMapping.swift` | repo-lead (verify only) | iOS, macOS, Android | oura sleep session mapping |
| `Packages/WhoopStore/Sources/WhoopStore/OuraStreamMapping.swift` | merge (both sides) | iOS, macOS, Android | oura stream mapping |
| `Packages/WhoopStore/Sources/WhoopStore/PairedDevice.swift` | merge (both sides) | iOS, macOS, Android | paired device |
| `Packages/WhoopStore/Sources/WhoopStore/RawOutbox.swift` | repo-lead (verify only) | iOS, macOS, Android | raw outbox |
| `Packages/WhoopStore/Sources/WhoopStore/Reads.swift` | merge (both sides) | iOS, macOS, Android | reads |
| `Packages/WhoopStore/Sources/WhoopStore/ScoreInputProvenanceStore.swift` | repo-lead (verify only) | iOS, macOS, Android | score input provenance store |
| `Packages/WhoopStore/Sources/WhoopStore/SleepMerge.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep merge |
| `Packages/WhoopStore/Sources/WhoopStore/SleepSessionDedup.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep session dedup |
| `Packages/WhoopStore/Sources/WhoopStore/SourceIdentity.swift` | repo-lead (verify only) | iOS, macOS, Android | source identity |
| `Packages/WhoopStore/Sources/WhoopStore/StandardHRMapping.swift` | repo-lead (verify only) | iOS, macOS, Android | standard hr mapping |
| `Packages/WhoopStore/Sources/WhoopStore/StoreProbeCounts.swift` | repo-lead (verify only) | iOS, macOS, Android | store probe counts |
| `Packages/WhoopStore/Sources/WhoopStore/StreamStore.swift` | merge (both sides) | iOS, macOS, Android | stream store |
| `Packages/WhoopStore/Sources/WhoopStore/TimestampHeal.swift` | merge (both sides) | iOS, macOS, Android | timestamp heal |
| `Packages/WhoopStore/Sources/WhoopStore/V18Aux.swift` | repo-lead (verify only) | iOS, macOS, Android | v18 aux |
| `Packages/WhoopStore/Sources/WhoopStore/Whoop5RRReadPolicy.swift` | repo-lead (verify only) | iOS, macOS, Android | whoop5 rr read policy |
| `Packages/WhoopStore/Sources/WhoopStore/WhoopLiveCapabilities.swift` | repo-lead (verify only) | iOS, macOS, Android | whoop live capabilities |
| `Packages/WhoopStore/Sources/WhoopStore/WhoopSerialIdentity.swift` | repo-lead (verify only) | iOS, macOS, Android | whoop serial identity |
| `Packages/WhoopStore/Sources/WhoopStore/WhoopStore.swift` | merge (both sides) | iOS, macOS, Android | whoop store |

### AI coach "Svea" (2)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Strand/AI/AICoach.swift` | aura-lead | iOS, macOS, Android | ai coach |
| `Strand/AI/AIProvider.swift` | aura-lead | iOS, macOS, Android | ai provider |

### AI providers (Svea) (4)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Strand/AI/Providers/Anthropic.swift` | minor | iOS, macOS, Android | anthropic |
| `Strand/AI/Providers/Custom.swift` | merge (both sides) | iOS, macOS, Android | custom |
| `Strand/AI/Providers/Gemini.swift` | repo-lead (verify only) | iOS, macOS, Android | gemini |
| `Strand/AI/Providers/OpenAI.swift` | repo-lead (verify only) | iOS, macOS, Android | open ai |

### App shell / root views (15)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Strand/App/ActiveWorkoutClock.swift` | repo-lead (verify only) | iOS, macOS, Android | active workout clock |
| `Strand/App/ActiveWorkoutPersistence.swift` | aura-lead | iOS, macOS, Android | active workout persistence |
| `Strand/App/AppClock.swift` | repo-lead (verify only) | iOS, macOS, Android | app clock |
| `Strand/App/AppLanguage.swift` | repo-lead (verify only) | iOS, macOS, Android | app language |
| `Strand/App/AppModel.swift` | merge (both sides) | iOS, macOS, Android | app model |
| `Strand/App/ContentView.swift` | repo-lead (verify only) | iOS, macOS, Android | content view |
| `Strand/App/GpsWorkoutRecorder.swift` | merge (both sides) | iOS, macOS, Android | gps workout recorder |
| `Strand/App/LiveSessionRunner.swift` | merge (both sides) | iOS, macOS, Android | live session runner |
| `Strand/App/MoreSectionPrefs.swift` | repo-lead (verify only) | iOS, macOS, Android | more section prefs |
| `Strand/App/NavRouter.swift` | merge (both sides) | iOS, macOS, Android | nav router |
| `Strand/App/RootView.swift` | merge (both sides) | iOS, macOS, Android | root view |
| `Strand/App/StrandApp.swift` | merge (both sides) | iOS, macOS, Android | strand app |
| `Strand/App/TabRoute.swift` | repo-lead (verify only) | iOS, macOS, Android | tab route |
| `Strand/App/Terms.swift` | repo-lead (verify only) | iOS, macOS, Android | terms |
| `Strand/App/TermsGateView.swift` | aura-lead | iOS, macOS, Android | terms gate view |

### BLE / CoreBluetooth (37)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Strand/BLE/BLEManager.swift` | merge (both sides) | iOS, macOS, Android | ble manager |
| `Strand/BLE/BackfillPolicy.swift` | merge (both sides) | iOS, macOS, Android | backfill policy |
| `Strand/BLE/ClientHelloOutcome.swift` | repo-lead (verify only) | iOS, macOS, Android | client hello outcome |
| `Strand/BLE/Commands.swift` | merge (both sides) | iOS, macOS, Android | commands |
| `Strand/BLE/DisUnbondedRead.swift` | repo-lead (verify only) | iOS, macOS, Android | dis unbonded read |
| `Strand/BLE/ExperimentalBrand.swift` | repo-lead (verify only) | iOS, macOS, Android | experimental brand |
| `Strand/BLE/FTMSSource.swift` | merge (both sides) | iOS, macOS, Android | ftms source |
| `Strand/BLE/FirmwareAttribution.swift` | repo-lead (verify only) | iOS, macOS, Android | firmware attribution |
| `Strand/BLE/FrameRouter.swift` | merge (both sides) | iOS, macOS, Android | frame router |
| `Strand/BLE/GarminBroadcast.swift` | repo-lead (verify only) | iOS, macOS, Android | garmin broadcast |
| `Strand/BLE/GattCapability.swift` | repo-lead (verify only) | iOS, macOS, Android | gatt capability |
| `Strand/BLE/HelloSuppression.swift` | repo-lead (verify only) | iOS, macOS, Android | hello suppression |
| `Strand/BLE/HrBroadcaster.swift` | merge (both sides) | iOS, macOS, Android | hr broadcaster |
| `Strand/BLE/HuamiHRSource.swift` | merge (both sides) | iOS, macOS, Android | huami hr source |
| `Strand/BLE/HuamiHeartRate.swift` | repo-lead (verify only) | iOS, macOS, Android | huami heart rate |
| `Strand/BLE/LastSyncAttribution.swift` | repo-lead (verify only) | iOS, macOS, Android | last sync attribution |
| `Strand/BLE/LiveHRSource.swift` | repo-lead (verify only) | iOS, macOS, Android | live hr source |
| `Strand/BLE/LiveHeartRateReadability.swift` | repo-lead (verify only) | iOS, macOS, Android | live heart rate readability |
| `Strand/BLE/LiveState.swift` | merge (both sides) | iOS, macOS, Android | live state |
| `Strand/BLE/OuraActivityDump.swift` | repo-lead (verify only) | iOS, macOS, Android | oura activity dump |
| `Strand/BLE/OuraLiveSource.swift` | merge (both sides) | iOS, macOS, Android | oura live source |
| `Strand/BLE/OuraMotionDump.swift` | repo-lead (verify only) | iOS, macOS, Android | oura motion dump |
| `Strand/BLE/OuraRawDump.swift` | repo-lead (verify only) | iOS, macOS, Android | oura raw dump |
| `Strand/BLE/OuraRealStepsDump.swift` | repo-lead (verify only) | iOS, macOS, Android | oura real steps dump |
| `Strand/BLE/PendingConnectReplay.swift` | repo-lead (verify only) | iOS, macOS, Android | pending connect replay |
| `Strand/BLE/PendingSyncDiagnostic.swift` | repo-lead (verify only) | iOS, macOS, Android | pending sync diagnostic |
| `Strand/BLE/PuffinDeepBufferLog.swift` | repo-lead (verify only) | iOS, macOS, Android | puffin deep buffer log |
| `Strand/BLE/PuffinEventLog.swift` | repo-lead (verify only) | iOS, macOS, Android | puffin event log |
| `Strand/BLE/PuffinExperiment.swift` | merge (both sides) | iOS, macOS, Android | puffin experiment |
| `Strand/BLE/PuffinFrameRecorder.swift` | repo-lead (verify only) | iOS, macOS, Android | puffin frame recorder |
| `Strand/BLE/SourceCoordinator.swift` | merge (both sides) | iOS, macOS, Android | source coordinator |
| `Strand/BLE/StandardHRSource.swift` | merge (both sides) | iOS, macOS, Android | standard hr source |
| `Strand/BLE/StandardHeartRate.swift` | repo-lead (verify only) | iOS, macOS, Android | standard heart rate |
| `Strand/BLE/StrapLogArchive.swift` | repo-lead (verify only) | iOS, macOS, Android | strap log archive |
| `Strand/BLE/StuckStrapDetector.swift` | repo-lead (verify only) | iOS, macOS, Android | stuck strap detector |
| `Strand/BLE/Whoop4BondAck.swift` | repo-lead (verify only) | iOS, macOS, Android | whoop4 bond ack |
| `Strand/BLE/WhoopModel.swift` | repo-lead (verify only) | iOS, macOS, Android | whoop model |

### Data collection (10)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Strand/Collect/Backfiller.swift` | merge (both sides) | iOS, macOS, Android | backfiller |
| `Strand/Collect/ClockCorrelation.swift` | repo-lead (verify only) | iOS, macOS, Android | clock correlation |
| `Strand/Collect/ClockPolicy.swift` | minor | iOS, macOS, Android | clock policy |
| `Strand/Collect/Collector.swift` | merge (both sides) | iOS, macOS, Android | collector |
| `Strand/Collect/ImuSessionFileStore.swift` | minor | iOS, macOS, Android | imu session file store |
| `Strand/Collect/PrunePolicy.swift` | minor | iOS, macOS, Android | prune policy |
| `Strand/Collect/RawCaptureWindow.swift` | minor | iOS, macOS, Android | raw capture window |
| `Strand/Collect/RawDataSessionStore.swift` | minor | iOS, macOS, Android | raw data session store |
| `Strand/Collect/RawHistoryArchive.swift` | merge (both sides) | iOS, macOS, Android | raw history archive |
| `Strand/Collect/StorePaths.swift` | repo-lead (verify only) | iOS, macOS, Android | store paths |

### Data layer (Repository) (59)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Strand/Data/AppleDemoSeeder.swift` | merge (both sides) | iOS, macOS, Android | apple demo seeder |
| `Strand/Data/AppleHealthImport.swift` | repo-lead (verify only) | iOS, macOS, Android | apple health import |
| `Strand/Data/AppleWatchDevice.swift` | repo-lead (verify only) | iOS, macOS, Android | apple watch device |
| `Strand/Data/BackupSync.swift` | merge (both sides) | iOS, macOS, Android | backup sync |
| `Strand/Data/BehaviorStore.swift` | aura-lead | iOS, macOS, Android | behavior store |
| `Strand/Data/CaffeineLog.swift` | merge (both sides) | iOS, macOS, Android | caffeine log |
| `Strand/Data/CsvExport.swift` | merge (both sides) | iOS, macOS, Android | csv export |
| `Strand/Data/CycleTrackingStore.swift` | repo-lead (verify only) | iOS, macOS, Android | cycle tracking store |
| `Strand/Data/DataBackup.swift` | merge (both sides) | iOS, macOS, Android | data backup |
| `Strand/Data/DayCycleIntelligenceIntegration.swift` | aura-lead | iOS, macOS, Android | day cycle intelligence integration |
| `Strand/Data/DaytimeStressMode.swift` | repo-lead (verify only) | iOS, macOS, Android | daytime stress mode |
| `Strand/Data/DemoDayHarness.swift` | repo-lead (verify only) | iOS, macOS, Android | demo day harness |
| `Strand/Data/DemoSyncHarness.swift` | repo-lead (verify only) | iOS, macOS, Android | demo sync harness |
| `Strand/Data/DeviceRegistry.swift` | repo-lead (verify only) | iOS, macOS, Android | device registry |
| `Strand/Data/EditableLayoutDraft.swift` | minor | iOS, macOS, Android | editable layout draft |
| `Strand/Data/HapticPrefs.swift` | repo-lead (verify only) | iOS, macOS, Android | haptic prefs |
| `Strand/Data/HealthSyncRefreshCoordinator.swift` | minor | iOS, macOS, Android | health sync refresh coordinator |
| `Strand/Data/HealthWritebackSchedulePolicy.swift` | minor | iOS, macOS, Android | health writeback schedule policy |
| `Strand/Data/HydrationStore.swift` | merge (both sides) | iOS, macOS, Android | hydration store |
| `Strand/Data/InactivityPrefs.swift` | merge (both sides) | iOS, macOS, Android | inactivity prefs |
| `Strand/Data/IntelligenceEngine.swift` | merge (both sides) | iOS, macOS, Android | intelligence engine |
| `Strand/Data/JournalCatalog.swift` | aura-lead | iOS, macOS, Android | journal catalog |
| `Strand/Data/KeyMetricPrefs.swift` | merge (both sides) | iOS, macOS, Android | key metric prefs |
| `Strand/Data/LiftBannerPushPolicy.swift` | repo-lead (verify only) | iOS, macOS, Android | lift banner push policy |
| `Strand/Data/LiftFormat.swift` | repo-lead (verify only) | iOS, macOS, Android | lift format |
| `Strand/Data/LiftMuscleNames.swift` | repo-lead (verify only) | iOS, macOS, Android | lift muscle names |
| `Strand/Data/LiftSessionController.swift` | aura-lead | iOS, macOS, Android | lift session controller |
| `Strand/Data/LiftSessionEngine.swift` | merge (both sides) | iOS, macOS, Android | lift session engine |
| `Strand/Data/LiftSessionPersistence.swift` | merge (both sides) | iOS, macOS, Android | lift session persistence |
| `Strand/Data/LiveHRBannerInputs.swift` | repo-lead (verify only) | iOS, macOS, Android | live hr banner inputs |
| `Strand/Data/LiveHRBannerLifecycle.swift` | repo-lead (verify only) | iOS, macOS, Android | live hr banner lifecycle |
| `Strand/Data/LiveHRBannerPushPolicy.swift` | repo-lead (verify only) | iOS, macOS, Android | live hr banner push policy |
| `Strand/Data/MetricCatalog.swift` | merge (both sides) | iOS, macOS, Android | metric catalog |
| `Strand/Data/MoodStore.swift` | repo-lead (verify only) | iOS, macOS, Android | mood store |
| `Strand/Data/NotificationSettingsStore.swift` | repo-lead (verify only) | iOS, macOS, Android | notification settings store |
| `Strand/Data/Profile.swift` | aura-lead | iOS, macOS, Android | profile |
| `Strand/Data/RecentSportsPrefs.swift` | repo-lead (verify only) | iOS, macOS, Android | recent sports prefs |
| `Strand/Data/Repository.swift` | merge (both sides) | iOS, macOS, Android | repository |
| `Strand/Data/RescoreBackgroundPolicy.swift` | repo-lead (verify only) | iOS, macOS, Android | rescore background policy |
| `Strand/Data/RollingStepsAverage.swift` | minor | iOS, macOS, Android | rolling steps average |
| `Strand/Data/ShortcutHealthExport.swift` | merge (both sides) | iOS, macOS, Android | shortcut health export |
| `Strand/Data/ShortcutHealthImport.swift` | repo-lead (verify only) | iOS, macOS, Android | shortcut health import |
| `Strand/Data/SkinTempBackfillWalker.swift` | merge (both sides) | iOS, macOS, Android | skin temp backfill walker |
| `Strand/Data/SleepLayoutPrefs.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep layout prefs |
| `Strand/Data/SleepMark.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep mark |
| `Strand/Data/StressDayCurve.swift` | merge (both sides) | iOS, macOS, Android | stress day curve |
| `Strand/Data/TodayLayoutPrefs.swift` | merge (both sides) | iOS, macOS, Android | today layout prefs |
| `Strand/Data/UnescalatedWork.swift` | repo-lead (verify only) | iOS, macOS, Android | unescalated work |
| `Strand/Data/Units.swift` | merge (both sides) | iOS, macOS, Android | units |
| `Strand/Data/UpdateStore.swift` | aura-lead | iOS, macOS, Android | update store |
| `Strand/Data/WatchSessionBridge.swift` | merge (both sides) | iOS, macOS, Android | watch session bridge |
| `Strand/Data/WearableImporter.swift` | aura-lead | iOS, macOS, Android | wearable importer |
| `Strand/Data/WhoopImporter.swift` | aura-lead | iOS, macOS, Android | whoop importer |
| `Strand/Data/WidgetAnchorMemo.swift` | repo-lead (verify only) | iOS, macOS, Android | widget anchor memo |
| `Strand/Data/WorkoutCatalog.swift` | merge (both sides) | iOS, macOS, Android | workout catalog |
| `Strand/Data/WorkoutPedometer.swift` | repo-lead (verify only) | iOS, macOS, Android | workout pedometer |
| `Strand/Data/WorkoutSource.swift` | merge (both sides) | iOS, macOS, Android | workout source |
| `Strand/Data/WorkoutZones.swift` | repo-lead (verify only) | iOS, macOS, Android | workout zones |
| `Strand/Data/XiaomiImporter.swift` | repo-lead (verify only) | iOS, macOS, Android | xiaomi importer |

### Other (3)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Strand/Health/HealthExportMerge.swift` | repo-lead (verify only) | iOS, macOS, Android | health export merge |
| `Strand/MenuBar/MenuBarContent.swift` | repo-lead (verify only) | iOS, macOS, Android | menu bar content |
| `Strand/Onboarding/OnboardingWizard.swift` | merge (both sides) | iOS, macOS, Android | onboarding wizard |

### Liquid Today surface (5)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Strand/Liquid/LiquidCore.swift` | merge (both sides) | iOS, macOS, Android | liquid core |
| `Strand/Liquid/LiquidPrimitives.swift` | aura-lead | iOS, macOS, Android | liquid primitives |
| `Strand/Liquid/LiquidSky.swift` | merge (both sides) | iOS, macOS, Android | liquid sky |
| `Strand/Liquid/LiquidTodayView.swift` | merge (both sides) | iOS, macOS, Android | liquid today view |
| `Strand/Liquid/LiveSessionView.swift` | merge (both sides) | iOS, macOS, Android | live session view |

### Oura (experimental) (14)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Strand/Oura/AuthProvider.swift` | repo-lead (verify only) | iOS, macOS, Android | auth provider |
| `Strand/Oura/OuraAPIClient.swift` | repo-lead (verify only) | iOS, macOS, Android | oura api client |
| `Strand/Oura/OuraConfig.xcconfig` | repo-lead (verify only) | iOS, macOS, Android | OuraConfig.xcconfig |
| `Strand/Oura/OuraConnectModel.swift` | repo-lead (verify only) | iOS, macOS, Android | oura connect model |
| `Strand/Oura/OuraCredentials.swift` | minor | iOS, macOS, Android | oura credentials |
| `Strand/Oura/OuraError.swift` | minor | iOS, macOS, Android | oura error |
| `Strand/Oura/OuraOAuth.swift` | repo-lead (verify only) | iOS, macOS, Android | oura o auth |
| `Strand/Oura/OuraOAuthProvider.swift` | minor | iOS, macOS, Android | oura o auth provider |
| `Strand/Oura/OuraPresentationAnchor.swift` | minor | iOS, macOS, Android | oura presentation anchor |
| `Strand/Oura/OuraSecrets.example.xcconfig` | minor | iOS, macOS, Android | OuraSecrets.example.xcconfig |
| `Strand/Oura/OuraSyncCoordinator.swift` | repo-lead (verify only) | iOS, macOS, Android | oura sync coordinator |
| `Strand/Oura/OuraSyncResult.swift` | repo-lead (verify only) | iOS, macOS, Android | oura sync result |
| `Strand/Oura/OuraSyncWriter.swift` | repo-lead (verify only) | iOS, macOS, Android | oura sync writer |
| `Strand/Oura/OuraTokenStore.swift` | repo-lead (verify only) | iOS, macOS, Android | oura token store |

### App resources (assets, plist, entitlements) (1)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Strand/Resources/Localizable.xcstrings` | merge (both sides) | iOS, macOS | Localizable.xcstrings |

### Screens (shared iOS+macOS) (104)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Strand/Screens/AddDeviceWizard.swift` | merge (both sides) | iOS, macOS, Android | add device wizard |
| `Strand/Screens/AppleHealthView.swift` | aura-lead | iOS, macOS, Android | apple health view |
| `Strand/Screens/AppleWatchAboutView.swift` | repo-lead (verify only) | iOS, macOS, Android | apple watch about view |
| `Strand/Screens/AppleWatchSetupView.swift` | merge (both sides) | iOS, macOS, Android | apple watch setup view |
| `Strand/Screens/AsleepDurationCard.swift` | repo-lead (verify only) | iOS, macOS, Android | asleep duration card |
| `Strand/Screens/AutoWorkoutCard.swift` | repo-lead (verify only) | iOS, macOS, Android | auto workout card |
| `Strand/Screens/AutomationsView.swift` | aura-lead | iOS, macOS, Android | automations view |
| `Strand/Screens/BackupSyncView.swift` | merge (both sides) | iOS, macOS, Android | backup sync view |
| `Strand/Screens/BiofeedbackController.swift` | merge (both sides) | iOS, macOS, Android | biofeedback controller |
| `Strand/Screens/BiofeedbackPrefs.swift` | repo-lead (verify only) | iOS, macOS, Android | biofeedback prefs |
| `Strand/Screens/BodyClockDialCard.swift` | repo-lead (verify only) | iOS, macOS, Android | body clock dial card |
| `Strand/Screens/BodyClockDialSection.swift` | repo-lead (verify only) | iOS, macOS, Android | body clock dial section |
| `Strand/Screens/BreathingView.swift` | merge (both sides) | iOS, macOS, Android | breathing view |
| `Strand/Screens/CaffeineLogCard.swift` | repo-lead (verify only) | iOS, macOS, Android | caffeine log card |
| `Strand/Screens/ChargeBreakdownFormat.swift` | merge (both sides) | iOS, macOS, Android | charge breakdown format |
| `Strand/Screens/ChargeBreakdownWiring.swift` | repo-lead (verify only) | iOS, macOS, Android | charge breakdown wiring |
| `Strand/Screens/CoachLauncherSheet.swift` | repo-lead (verify only) | iOS, macOS, Android | coach launcher sheet |
| `Strand/Screens/CoachMarkdownTheme.swift` | minor | iOS, macOS, Android | coach markdown theme |
| `Strand/Screens/CoachPrompts.swift` | minor | iOS, macOS, Android | coach prompts |
| `Strand/Screens/CoachSettingsView.swift` | repo-lead (verify only) | iOS, macOS, Android | coach settings view |
| `Strand/Screens/CoachView.swift` | merge (both sides) | iOS, macOS, Android | coach view |
| `Strand/Screens/CompareView.swift` | merge (both sides) | iOS, macOS, Android | compare view |
| `Strand/Screens/ConsistencyCard.swift` | repo-lead (verify only) | iOS, macOS, Android | consistency card |
| `Strand/Screens/CoupledView.swift` | merge (both sides) | iOS, macOS, Android | coupled view |
| `Strand/Screens/DashboardCards.swift` | merge (both sides) | iOS, macOS, Android | dashboard cards |
| `Strand/Screens/DataSourcesView.swift` | merge (both sides) | iOS, macOS, Android | data sources view |
| `Strand/Screens/DevicesView.swift` | merge (both sides) | iOS, macOS, Android | devices view |
| `Strand/Screens/EditableLayoutList.swift` | repo-lead (verify only) | iOS, macOS, Android | editable layout list |
| `Strand/Screens/FullDayChartView.swift` | merge (both sides) | iOS, macOS, Android | full day chart view |
| `Strand/Screens/FusedRecordView.swift` | merge (both sides) | iOS, macOS, Android | fused record view |
| `Strand/Screens/HRVSnapshotView.swift` | merge (both sides) | iOS, macOS, Android | hrv snapshot view |
| `Strand/Screens/HealthAlertBanner.swift` | minor | iOS, macOS, Android | health alert banner |
| `Strand/Screens/HealthView.swift` | merge (both sides) | iOS, macOS, Android | health view |
| `Strand/Screens/HostedCards.swift` | merge (both sides) | iOS, macOS, Android | hosted cards |
| `Strand/Screens/HostedTrendCard.swift` | repo-lead (verify only) | iOS, macOS, Android | hosted trend card |
| `Strand/Screens/HoursVsNeededCard.swift` | repo-lead (verify only) | iOS, macOS, Android | hours vs needed card |
| `Strand/Screens/HowNoopWorksView.swift` | repo-lead (verify only) | iOS, macOS, Android | how noop works view |
| `Strand/Screens/HydrationView.swift` | merge (both sides) | iOS, macOS, Android | hydration view |
| `Strand/Screens/InsightsHubView.swift` | merge (both sides) | iOS, macOS, Android | insights hub view |
| `Strand/Screens/InsightsView.swift` | merge (both sides) | iOS, macOS, Android | insights view |
| `Strand/Screens/IntelligenceView.swift` | merge (both sides) | iOS, macOS, Android | intelligence view |
| `Strand/Screens/IntervalTimerView.swift` | merge (both sides) | iOS, macOS, Android | interval timer view |
| `Strand/Screens/JournalLogCard.swift` | aura-lead | iOS, macOS, Android | journal log card |
| `Strand/Screens/JournalReminderCard.swift` | merge (both sides) | iOS, macOS, Android | journal reminder card |
| `Strand/Screens/KeyboardDismiss.swift` | repo-lead (verify only) | iOS, macOS, Android | keyboard dismiss |
| `Strand/Screens/LabBookView.swift` | aura-lead | iOS, macOS, Android | lab book view |
| `Strand/Screens/LiftExercisePicking.swift` | repo-lead (verify only) | iOS, macOS, Android | lift exercise picking |
| `Strand/Screens/LiftLiveReadouts.swift` | repo-lead (verify only) | iOS, macOS, Android | lift live readouts |
| `Strand/Screens/LiftLogView.swift` | merge (both sides) | iOS, macOS, Android | lift log view |
| `Strand/Screens/LiftProgramEditorSheet.swift` | repo-lead (verify only) | iOS, macOS, Android | lift program editor sheet |
| `Strand/Screens/LiftProgramImportSheet.swift` | repo-lead (verify only) | iOS, macOS, Android | lift program import sheet |
| `Strand/Screens/LiftProgramItemSheet.swift` | merge (both sides) | iOS, macOS, Android | lift program item sheet |
| `Strand/Screens/LiftSessionBar.swift` | repo-lead (verify only) | iOS, macOS, Android | lift session bar |
| `Strand/Screens/LiftSessionDetailSheet.swift` | repo-lead (verify only) | iOS, macOS, Android | lift session detail sheet |
| `Strand/Screens/LiftSessionEditSheet.swift` | repo-lead (verify only) | iOS, macOS, Android | lift session edit sheet |
| `Strand/Screens/LiftSessionExerciseSheet.swift` | repo-lead (verify only) | iOS, macOS, Android | lift session exercise sheet |
| `Strand/Screens/LiftSessionView.swift` | merge (both sides) | iOS, macOS, Android | lift session view |
| `Strand/Screens/LiveView.swift` | merge (both sides) | iOS, macOS, Android | live view |
| `Strand/Screens/LiveWorkoutView.swift` | merge (both sides) | iOS, macOS, Android | live workout view |
| `Strand/Screens/ManualWorkoutSheet.swift` | merge (both sides) | iOS, macOS, Android | manual workout sheet |
| `Strand/Screens/MarkerEditorView.swift` | repo-lead (verify only) | iOS, macOS, Android | marker editor view |
| `Strand/Screens/MetricExplorerView.swift` | merge (both sides) | iOS, macOS, Android | metric explorer view |
| `Strand/Screens/MindSection.swift` | repo-lead (verify only) | iOS, macOS, Android | mind section |
| `Strand/Screens/NightDetailCard.swift` | repo-lead (verify only) | iOS, macOS, Android | night detail card |
| `Strand/Screens/NoopLimitationsView.swift` | repo-lead (verify only) | iOS, macOS, Android | noop limitations view |
| `Strand/Screens/NotificationSettingsView.swift` | repo-lead (verify only) | iOS, macOS, Android | notification settings view |
| `Strand/Screens/PowerSavingView.swift` | repo-lead (verify only) | iOS, macOS, Android | power saving view |
| `Strand/Screens/ProfileAvatarView.swift` | repo-lead (verify only) | iOS, macOS, Android | profile avatar view |
| `Strand/Screens/RRPacketObserver.swift` | repo-lead (verify only) | iOS, macOS, Android | rr packet observer |
| `Strand/Screens/RawDataCollectorView.swift` | minor | iOS, macOS, Android | raw data collector view |
| `Strand/Screens/RhythmView.swift` | merge (both sides) | iOS, macOS, Android | rhythm view |
| `Strand/Screens/ScoringGuideView.swift` | merge (both sides) | iOS, macOS, Android | scoring guide view |
| `Strand/Screens/ScreenScaffold.swift` | aura-lead | iOS, macOS, Android | screen scaffold |
| `Strand/Screens/SettingsView.swift` | merge (both sides) | iOS, macOS, Android | settings view |
| `Strand/Screens/SkinTempCardsView.swift` | merge (both sides) | iOS, macOS, Android | skin temp cards view |
| `Strand/Screens/SleepCustomizationSheet.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep customization sheet |
| `Strand/Screens/SleepDebtLedgerCard.swift` | repo-lead (verify only) | iOS, macOS, Android | sleep debt ledger card |
| `Strand/Screens/SleepModel.swift` | merge (both sides) | iOS, macOS, Android | sleep model |
| `Strand/Screens/SleepResultChange.swift` | minor | iOS, macOS, Android | sleep result change |
| `Strand/Screens/SleepView.swift` | merge (both sides) | iOS, macOS, Android | sleep view |
| `Strand/Screens/SmartAlarmView.swift` | merge (both sides) | iOS, macOS, Android | smart alarm view |
| `Strand/Screens/StagesCard.swift` | merge (both sides) | iOS, macOS, Android | stages card |
| `Strand/Screens/StagesVsTypicalCard.swift` | repo-lead (verify only) | iOS, macOS, Android | stages vs typical card |
| `Strand/Screens/StorageView.swift` | repo-lead (verify only) | iOS, macOS, Android | storage view |
| `Strand/Screens/StressCheckInCard.swift` | repo-lead (verify only) | iOS, macOS, Android | stress check in card |
| `Strand/Screens/StressView.swift` | merge (both sides) | iOS, macOS, Android | stress view |
| `Strand/Screens/TestCentreView.swift` | merge (both sides) | iOS, macOS, Android | test centre view |
| `Strand/Screens/TodayCustomizationMetadata.swift` | minor | iOS, macOS, Android | today customization metadata |
| `Strand/Screens/TodayCustomizationSheet.swift` | aura-lead | iOS, macOS, Android | today customization sheet |
| `Strand/Screens/TodayView.swift` | merge (both sides) | iOS, macOS, Android | today view |
| `Strand/Screens/TrainingLoadCard.swift` | repo-lead (verify only) | iOS, macOS, Android | training load card |
| `Strand/Screens/TrainingLoadChart.swift` | repo-lead (verify only) | iOS, macOS, Android | training load chart |
| `Strand/Screens/TrendsReportView.swift` | merge (both sides) | iOS, macOS, Android | trends report view |
| `Strand/Screens/TrendsView.swift` | merge (both sides) | iOS, macOS, Android | trends view |
| `Strand/Screens/UpdatesInboxView.swift` | repo-lead (verify only) | iOS, macOS, Android | updates inbox view |
| `Strand/Screens/V5PillarHosts.swift` | repo-lead (verify only) | iOS, macOS, Android | v5 pillar hosts |
| `Strand/Screens/VitalSignsSummary.swift` | merge (both sides) | iOS, macOS, Android | vital signs summary |
| `Strand/Screens/WeeklyDigestView.swift` | merge (both sides) | iOS, macOS, Android | weekly digest view |
| `Strand/Screens/WhatsNewView.swift` | aura-lead | iOS, macOS, Android | whats new view |
| `Strand/Screens/WorkoutDetailView.swift` | merge (both sides) | iOS, macOS, Android | workout detail view |
| `Strand/Screens/WorkoutSelectionScreen.swift` | aura-lead | iOS, macOS, Android | workout selection screen |
| `Strand/Screens/WorkoutStartControl.swift` | merge (both sides) | iOS, macOS, Android | workout start control |
| `Strand/Screens/WorkoutsView.swift` | merge (both sides) | iOS, macOS, Android | workouts view |
| `Strand/Screens/XiaomiBandView.swift` | repo-lead (verify only) | iOS, macOS, Android | xiaomi band view |

### System services (notifications, updates, background) (36)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `Strand/System/AppChangelog.swift` | aura-lead | iOS, macOS, Android | app changelog |
| `Strand/System/BackgroundImageStore.swift` | repo-lead (verify only) | iOS, macOS, Android | background image store |
| `Strand/System/BatteryGuidedCapture.swift` | repo-lead (verify only) | iOS, macOS, Android | battery guided capture |
| `Strand/System/BatteryNotifier.swift` | merge (both sides) | iOS, macOS, Android | battery notifier |
| `Strand/System/CoachBriefScheduler.swift` | merge (both sides) | iOS, macOS, Android | coach brief scheduler |
| `Strand/System/DebugDataDiagnostics.swift` | merge (both sides) | iOS, macOS, Android | debug data diagnostics |
| `Strand/System/DeviceInventory.swift` | repo-lead (verify only) | iOS, macOS, Android | device inventory |
| `Strand/System/DisplayPerformanceMonitor.swift` | merge (both sides) | iOS, macOS, Android | display performance monitor |
| `Strand/System/DisplayScreenshot.swift` | repo-lead (verify only) | iOS, macOS, Android | display screenshot |
| `Strand/System/DocumentPicker.swift` | merge (both sides) | iOS, macOS, Android | document picker |
| `Strand/System/FileExport.swift` | repo-lead (verify only) | iOS, macOS, Android | file export |
| `Strand/System/GuidedCapture.swift` | repo-lead (verify only) | iOS, macOS, Android | guided capture |
| `Strand/System/HealthSyncStats.swift` | repo-lead (verify only) | iOS, macOS, Android | health sync stats |
| `Strand/System/IOSDiagnostics.swift` | merge (both sides) | iOS, macOS, Android | ios diagnostics |
| `Strand/System/IllnessNotifier.swift` | repo-lead (verify only) | iOS, macOS, Android | illness notifier |
| `Strand/System/MacActions.swift` | aura-lead | iOS, macOS, Android | mac actions |
| `Strand/System/MetricKitLine.swift` | repo-lead (verify only) | iOS, macOS, Android | metric kit line |
| `Strand/System/NOOPAppIntents.swift` | repo-lead (verify only) | iOS, macOS, Android | noop app intents |
| `Strand/System/NoopScratch.swift` | repo-lead (verify only) | iOS, macOS, Android | noop scratch |
| `Strand/System/NotificationPresenter.swift` | aura-lead | iOS, macOS, Android | notification presenter |
| `Strand/System/Platform.swift` | repo-lead (verify only) | iOS, macOS, Android | platform |
| `Strand/System/ProjectInfo.swift` | minor | iOS, macOS, Android | project info |
| `Strand/System/ReportReviewGate.swift` | repo-lead (verify only) | iOS, macOS, Android | report review gate |
| `Strand/System/RescoreBackgroundScheduler.swift` | merge (both sides) | iOS, macOS, Android | rescore background scheduler |
| `Strand/System/ScheduledDebugExport.swift` | merge (both sides) | iOS, macOS, Android | scheduled debug export |
| `Strand/System/StrainTargetNotifier.swift` | aura-lead | iOS, macOS, Android | strain target notifier |
| `Strand/System/SyncActivityCopy.swift` | minor | iOS, macOS, Android | sync activity copy |
| `Strand/System/TestBundleAssembler.swift` | repo-lead (verify only) | iOS, macOS, Android | test bundle assembler |
| `Strand/System/TestBundleMeta.swift` | repo-lead (verify only) | iOS, macOS, Android | test bundle meta |
| `Strand/System/TestCentre.swift` | merge (both sides) | iOS, macOS, Android | test centre |
| `Strand/System/TestCentreReport.swift` | repo-lead (verify only) | iOS, macOS, Android | test centre report |
| `Strand/System/TestReportFlow.swift` | repo-lead (verify only) | iOS, macOS, Android | test report flow |
| `Strand/System/TrendsReportRenderer.swift` | repo-lead (verify only) | iOS, macOS, Android | trends report renderer |
| `Strand/System/UpdateAvailability.swift` | aura-lead | iOS, macOS, Android | update availability |
| `Strand/System/UpdateChecker.swift` | aura-lead | iOS, macOS, Android | update checker |
| `Strand/System/WindDownNudge.swift` | merge (both sides) | iOS, macOS, Android | wind down nudge |

### iOS app shell / tabs (5)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `StrandiOS/App/AppModel+iOS.swift` | aura-lead | iOS, Android | app model+i os |
| `StrandiOS/App/CoachVoiceInput.swift` | repo-lead (verify only) | iOS, Android | coach voice input |
| `StrandiOS/App/RootTabView.swift` | merge (both sides) | iOS, Android | root tab view |
| `StrandiOS/App/ShortcutExportSettingsView.swift` | aura-lead | iOS, Android | shortcut export settings view |
| `StrandiOS/App/StrandiOSApp.swift` | merge (both sides) | iOS, Android | strandi os app |

### HealthKit (iOS) (2)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `StrandiOS/Health/HealthKitBridge.swift` | merge (both sides) | iOS, Android | health kit bridge |
| `StrandiOS/Health/HealthWritebackBackgroundScheduler.swift` | repo-lead (verify only) | iOS, Android | health writeback background scheduler |

### iOS resources (icons, Rive, plist) (4)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `StrandiOS/Resources/Assets.xcassets/AppIcon-Navy.appiconset/Contents.json` | aura-lead | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/AppIcon.appiconset/Contents.json` | aura-lead | iOS | JSON data: Contents |
| `StrandiOS/Resources/Assets.xcassets/AppIcon.appiconset/icon_1024.png` | minor | iOS | image asset: icon_1024 |
| `StrandiOS/Resources/Info.plist` | aura-lead | iOS | Info.plist |

### iOS system (intents, sync, streaks) (6)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `StrandiOS/System/ExportWorkoutRouteIntent.swift` | repo-lead (verify only) | iOS, Android | export workout route intent |
| `StrandiOS/System/HomeScreenQuickActions.swift` | repo-lead (verify only) | iOS, Android | home screen quick actions |
| `StrandiOS/System/MetricKitLog.swift` | minor | iOS, Android | metric kit log |
| `StrandiOS/System/NOOPAppIntents.swift` | aura-lead | iOS, Android | noop app intents |
| `StrandiOS/System/StaleBatteryBackgroundScheduler.swift` | repo-lead (verify only) | iOS, Android | stale battery background scheduler |
| `StrandiOS/System/SyncKeepAwake.swift` | minor | iOS, Android | sync keep awake |

### Live Activities (iOS) (4)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `StrandiOS/Widgets/LiftLiveActivityController.swift` | merge (both sides) | iOS, Android | lift live activity controller |
| `StrandiOS/Widgets/LiveActivityController.swift` | aura-lead | iOS, Android | live activity controller |
| `StrandiOS/Widgets/SyncLiveActivityController.swift` | repo-lead (verify only) | iOS, Android | sync live activity controller |
| `StrandiOS/Widgets/WidgetPublish.swift` | merge (both sides) | iOS, Android | widget publish |

### iOS shared (widgets/live-activity attrs) (7)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `StrandiOSShared/HrTrace.swift` | repo-lead (verify only) | iOS, Android | hr trace |
| `StrandiOSShared/LiftActivityAttributes.swift` | aura-lead | iOS, Android | lift activity attributes |
| `StrandiOSShared/LiveActivityAttributes.swift` | aura-lead | iOS, Android | live activity attributes |
| `StrandiOSShared/StressTrace.swift` | repo-lead (verify only) | iOS, Android | stress trace |
| `StrandiOSShared/SyncActivityAttributes.swift` | repo-lead (verify only) | iOS, Android | sync activity attributes |
| `StrandiOSShared/WidgetSnapshot.swift` | aura-lead | iOS, Android | widget snapshot |
| `StrandiOSShared/WidgetTelemetry.swift` | repo-lead (verify only) | iOS, Android | widget telemetry |

### Widgets / Live Activities (iOS) (8)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `StrandiOSWidgets/HeartRateWidget.swift` | merge (both sides) | iOS, Android | heart rate widget |
| `StrandiOSWidgets/Info.plist` | minor | iOS, Android | Info.plist |
| `StrandiOSWidgets/LiftLiveActivity.swift` | merge (both sides) | iOS, Android | lift live activity |
| `StrandiOSWidgets/NOOPLiveActivity.swift` | aura-lead | iOS, Android | noop live activity |
| `StrandiOSWidgets/NOOPWidget.swift` | merge (both sides) | iOS, Android | noop widget |
| `StrandiOSWidgets/NOOPWidgetBundle.swift` | aura-lead | iOS, Android | noop widget bundle |
| `StrandiOSWidgets/StressWidget.swift` | merge (both sides) | iOS, Android | stress widget |
| `StrandiOSWidgets/SyncLiveActivity.swift` | repo-lead (verify only) | iOS, Android | sync live activity |

### Build config (project.yml) (1)

| Path | Direction | Platforms | Description |
|---|---|---|---|
| `project.yml` | merge (both sides) | iOS, macOS | project.yml |

---

## 3. Deleted files: none

The 581 files present only in the repo (Android app, watchOS targets, docs, CI, Tools, tests, generated Xcode project, package tests) are all infrastructure the Aura source simply does not include — **keep them all; delete nothing**.

---

## 4. Config & metadata changes (from `project.yml`, `Config/`, Info.plist, entitlements)

| Item | Change | Platforms |
|---|---|---|
| Version | `MARKETING_VERSION` 12.1.0 → 12.1.1, `CURRENT_PROJECT_VERSION` 439 → 1239 (Android `versionName` must track: 12.1.0 → 12.1.1) | iOS, macOS, Android |
| App name (iOS) | `CFBundleName`/`CFBundleDisplayName`/`PRODUCT_NAME` NOOP → **Noop Aura**; `PRODUCT_MODULE_NAME: NOOP_Staging` | iOS |
| macOS app name | stays `NOOP` (Aura project.yml unchanged for the macOS target display name) | macOS |
| Widget bundle name | `CFBundleDisplayName` NOOP → `\u2800` (braille blank, hides extension name) | iOS |
| New SPM deps | Rive 6.28.1 (`RiveRuntime`), ZIPFoundation 0.9.20, SuperAgeCore 0.4.0 (superageapp/ios-core), local: Biscuit, StrandTraining, StrandFitness, ScaleProtocol | iOS, macOS, Android (native equivalents) |
| Embedded binary | `Vendor/Nomic/llama.xcframework` (llama.cpp) embedded in iOS app; Gemma license texts in `Vendor/Nomic/Resources` | iOS, Android (port) |
| Strict concurrency | `SWIFT_STRICT_CONCURRENCY: complete`; Debug `SWIFT_OPTIMIZATION_LEVEL: -O` (iOS Release `-Onone`) | iOS, macOS |
| macOS preBuild script | "Bundle optional tables" script added to the macOS `Strand` target (strain_raw_scale_lookup.json / recovery_to_strain.json) | macOS |
| iOS entitlements | healthkit, healthkit.background-delivery, application-groups `$(APP_GROUP_ID)` | iOS |
| Background tasks | `BGTaskSchedulerPermittedIdentifiers`: debugexport, sveaproactive, modelwatch, streakcheck, coachbrief, healthwriteback, rescore, stalebattery | iOS, Android (WorkManager equivalents) |
| New usage descriptions | NSCameraUsageDescription, NSPhotoLibraryAddUsageDescription, NSAlarmKitUsageDescription, NSSpeechRecognitionUsageDescription, NSMicrophoneUsageDescription, NSHealthShare/Update/Clinical, NSMotion, updated NSBluetooth/NSLocation/NSHealth texts; `LSApplicationQueriesSchemes: instagram-stories`; `NSSupportsLiveActivities`; `UIBackgroundModes: bluetooth-central, location, fetch, processing, workout-processing` | iOS, Android (manifest permissions) |
| Icons | 8 alternate app-icon sets (AppIcon-Aura, -Titanium, -Breathe, -Navy, -Orb, -Ember, -Frost, -Aurora) + `LaunchBackground.colorset` + `UILaunchScreen` | iOS, Android (adaptive icon) |
| Release exclusions | iOS Release `EXCLUDED_SOURCE_FILE_NAMES` trims 9 widget panel files from the app target | iOS |
| Watch/test targets | NOT in Aura project.yml (Aura is an iOS-only extract; repo keeps `NOOPWatch`, `NOOPWatchComplications`, `StrandTests`) | macOS (keep repo) |
| Bundle ID | `Config/BundleId.xcconfig` differs — repo has tracked wrapper + gitignored secrets mechanism; **merge, do not regress** | iOS, macOS |
| Docs | `LICENSE`, `NOTICE`, `TERMS.md` differ (Gemma/vendor notices, terms) | iOS, macOS, Android |

## 5. Ambiguities & open questions

1. **True merge, not a copy**: repo and Aura diverged in both directions (repo ahead on 552 files — e.g. OuraProtocol, most StrandAnalytics, TodayView). Port = apply Aura deltas without regressing repo-ahead content.
2. **On-device LLM**: Aura embeds `llama.xcframework` + Gemma (Google) license texts. Binary is a prebuilt third-party artifact — keep as-is on Apple; Android needs a native on-device LLM equivalent (llama.cpp NDK / Google AI Edge) or a flag-off path. License texts must ship alongside.
3. **SuperAgeCore** is an external SPM dependency (`superageapp/ios-core` 0.4.0) — third-party bio-age library; Android needs an equivalent implementation or reimplementation of its public surface.
4. **Rive** animations (`.riv`) — Rive has first-class Android support, so port is straightforward.
5. **Aura source omits** `NOOPWatch*`, `StrandTests`, `android/`, docs, CI — treat as "not included in extract", not deletions.
6. **`CFBundleDisplayName` braille-blank** for the widget bundle is an intentional trick to hide the extension name; replicate as-is.
7. **Build verification**: this environment is Linux without Xcode — `NOOPiOS`/`Strand` app-target builds and `StrandTests` cannot run locally; Swift pure packages can be built/tested if a toolchain is present; Android builds/ tests run via Gradle. What is verified will be stated in the final report.
8. **App rename on Android**: Aura renames the iOS app to "Noop Aura". For Android parity the label becomes "Noop Aura" (label string + `versionName` bump); bundle/application id unchanged (staging id preserved).

