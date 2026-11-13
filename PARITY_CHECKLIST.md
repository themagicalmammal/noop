# Aura 12.1.1 Android parity checklist

Status refers to the Android app on this branch, not whether a similarly named feature exists on an
Apple target.

| Aura area | Android status | Evidence / next work |
|---|---|---|
| 12.1.1 reinstall-resilient backups | Existing equivalent | Android already writes dated `.noopbak` files to a user-selected SAF folder that can be synced by a provider; verify onboarding and recovery affordances against Aura. |
| Fitness hub and training dashboards | Partial | Added Fitness as a top-level Android tab with weekly activity, recent workouts, and links to tracking, goals, and imports; Aura's deeper training dashboards remain. |
| Goals | In progress | Android has local goals, starter/custom goal editing and progress, opt-in streaks with low-recovery/rest/freeze and pause rules, Monday/Tuesday prior-week recap, persistent weekly quests with tiered progress, one swap, starter fallback and recovery easing, streak milestone challenges, and persisted Aura-shaped chest/reward fields. Deterministic reward rolls cover milestone, quest, all-three quest, perfect-week and hard-week chests; freezes feed the streak bank. The Android reward collection lists the ten flames, exposes locked/owned state and equips earned flames for the streak progress accent. Orb, app-icon and widget reward uses and the chest opening presentation remain platform-specific follow-up work. |
| Activity and history import | Partial | Several importers exist; compare Aura's added formats/providers and implement gaps. |
| Smart scales | Partial | Android has foreground, receive-only broadcast support for Etekcity Fit 8S, AAA-series, Scale Up, QN and Yunmai X; readings persist to the shared `smart-scale` metric series and appear in local weigh-in history. Remaining Aura drivers, chart surfaces and hardware validation are still needed. |
| Sleep and strain improvements | Audit required | Compare each Aura analytics delta with Kotlin twin; preserve byte-identical stored/results contract. |
| Shortcuts and system controls | Partial / platform-specific | Compare Aura actions with Android App Shortcuts, tiles, notifications, and widgets. |
| In-app updates | Existing equivalent | Audit parity and ensure Android delivery stays user-controlled and source-compatible. |
| Svea AI coach additions | Partial | Existing coach exists; compare new capabilities and preserve default-off/offline boundaries. |
| Biscuit companion | Partial | Android bundles Aura's vector artboard and bindings and renders them through a native Canvas path/fill/gradient renderer with mood and chest-unlocked outfit selection in a Companion screen. The animation timeline (frame math, sampling, cubic/elastic easing, color lerp) is a bit-exact Kotlin twin of Aura's PersonalityTimeline, verified against a compiled Swift oracle, and the renderer applies Aura's static pose stack (eye base, visible, mood, veil, outfits) through that twin. A BiscuitController twin drives the live companion (mood/outfit/color crossfades, ghost veil, cloud drift, behavior scheduling, touch springs and reactions, scroll hold, arrive/depart visibility, pause) with a unit-tested pose stack; the artboard and bindings ship inside the APK. Widget face/flame/chest pictures, the chest-opening presentation, and richer clipping/feather rendering remain. |
| Localization, release identity | In progress | Version aligned to 12.1.1; app label changed to Noop Aura; audit all supported Android locales. |

## Completion rule

Each row is complete only after implementation, Android build, relevant unit tests, localization
audit, and a documented explanation for any Apple-only surface with no Android analogue.
