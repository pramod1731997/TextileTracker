# Textile Tracker

Offline-first Android powerloom production tracker built with Kotlin, Jetpack Compose Material 3 and Room.

## Build
Requires JDK 17 and Gradle 8.11.1:
```bash
gradle assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

A GitHub Actions workflow builds the debug APK on every push to `main` and on manual dispatch.

## Current foundation
- Room entities for warping machines, operators, galas, looms, beams, operator allocations, beam movements and TAKAs.
- Dashboard metrics, low-cut alerts and quick actions.
- Factory setup for machine ranges, operator directory, Gala Hamali rates and bulk loom generation.
- Beam register, loom mounting/freeing, TAKA entry and cut consumption.
- Payroll overview and stored movement/operator allocation data.

## Production hardening still required
Before using for live payroll, add transaction-level validations, complete shared-operator split editor, destination-specific Hamal selection, roll-prefix configuration, cut swapping, PDF/WhatsApp exports, JSON import/export, Supabase sync and automated tests. The current UI is an initial working foundation, not a validated accounting release.
