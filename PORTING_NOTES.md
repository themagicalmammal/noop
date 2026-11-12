# Aura 12.1.1 Android port

The Aura 12.1.1 Apple source is the behavior reference. Android keeps its native Kotlin, Compose,
Room, WorkManager, Health Connect, and SAF implementation. This is a feature port, not a source copy.

## Scope

Aura 12.1.0 introduced the Fitness hub, added import sources and smart scales, refined sleep and
strain, added system shortcuts/controls, in-app update delivery, and optional Svea, Biscuit, and
Goals features. Aura 12.1.1 added a reinstall-resilient backup onboarding path. Existing NOOP
Android equivalents count as complete only after their behavior is checked against Aura.

The Android project already has workout/live-session tracking, several importers, Health Connect,
in-app updates, and folder-based `.noopbak` backups. It did not have the Aura Goals feature. Goals is
the first added feature in this port. Its preferences stay local and default off; summaries use the
shared workout sources without adding a database migration.

## Port decisions

- Use Room and SharedPreferences for Android persistence; do not add a second database.
- Preserve Android SAF and provider-based backup behavior. The Android folder backup already
  supports user-selected sync folders, so Aura's iCloud-specific migration is not applicable.
- Keep BLE scale support experimental and issue-first. Do not add a scale write/control command.
- AI and companion features remain offline/default-off unless the user explicitly enables a provider
  or feature. Never include credentials from the Apple source.
- Keep all new UI on the Android design tokens and localize user-facing strings.

## Verification limits

Android builds and JVM tests run in this Linux environment. Apple targets and the Swift oracle are
not available here, so any newly mirrored pure logic must be compared against Aura source and has a
recorded limitation until checked with Swift on macOS.
