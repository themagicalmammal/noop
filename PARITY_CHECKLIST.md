# Aura 12.1.1 Android parity checklist

Status refers to the Android app on this branch, not whether a similarly named feature exists on an
Apple target.

| Aura area | Android status | Evidence / next work |
|---|---|---|
| 12.1.1 reinstall-resilient backups | Existing equivalent | Android already writes dated `.noopbak` files to a user-selected SAF folder that can be synced by a provider; verify onboarding and recovery affordances against Aura. |
| Fitness hub and training dashboards | Partial | Added Fitness as a top-level Android tab with weekly activity, recent workouts, and links to tracking, goals, and imports; Aura's deeper training dashboards remain. |
| Goals | In progress | Added local goal model/store, starter and custom goals, editing, progress, removal, and opt-in streaks with low-recovery/rest/freeze coverage, pause dates, and adjustable weekly rest days; remaining Aura weekly quests, recap, milestone challenges, and rewards. |
| Activity and history import | Partial | Several importers exist; compare Aura's added formats/providers and implement gaps. |
| Smart scales | Missing | Research protocol clean-room facts, then add experimental receive-only support with tests. |
| Sleep and strain improvements | Audit required | Compare each Aura analytics delta with Kotlin twin; preserve byte-identical stored/results contract. |
| Shortcuts and system controls | Partial / platform-specific | Compare Aura actions with Android App Shortcuts, tiles, notifications, and widgets. |
| In-app updates | Existing equivalent | Audit parity and ensure Android delivery stays user-controlled and source-compatible. |
| Svea AI coach additions | Partial | Existing coach exists; compare new capabilities and preserve default-off/offline boundaries. |
| Biscuit companion | Missing | Port behavior and artwork with Android-native rendering; no server dependency. |
| Localization, release identity | In progress | Version aligned to 12.1.1; app label changed to Noop Aura; audit all supported Android locales. |

## Completion rule

Each row is complete only after implementation, Android build, relevant unit tests, localization
audit, and a documented explanation for any Apple-only surface with no Android analogue.
