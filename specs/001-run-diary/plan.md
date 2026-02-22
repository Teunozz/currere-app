# Implementation Plan: Run Diary

**Branch**: `001-run-diary` | **Date**: 2026-02-22 | **Spec**: specs/001-run-diary/spec.md
**Input**: Feature specification from `/specs/001-run-diary/spec.md`

## Summary

Build Currere — a read-only Health Connect companion app with two screens: a run diary list showing all running sessions with key stats (distance, time, pace), and a run detail screen with summary stats, heart rate chart, pace chart, and per-km pace splits. The app uses Jetpack Compose with Material 3, reads data exclusively from Health Connect via the connect-client SDK, and follows a clean data/domain/ui layer separation without a DI framework.

## Technical Context

**Language/Version**: Kotlin 2.0.21
**Primary Dependencies**: Jetpack Compose (BOM 2025.01.01), Material 3, Health Connect connect-client 1.1.0, Vico 2.x (charting), Navigation Compose 2.8.x with Kotlin Serialization
**Storage**: Health Connect (read-only) — no local database, no file persistence
**Testing**: JUnit 4 for domain logic unit tests
**Target Platform**: Android 16 (SDK 36)
**Project Type**: Mobile app (single module)
**Performance Goals**: <2s initial run list load, <1s detail screen load, 60fps list scrolling
**Constraints**: Fully offline, no network calls, no cloud sync, no data written to Health Connect
**Scale/Scope**: Single user, 2 screens + permission flow, supports 500+ exercise sessions

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Privacy & Offline-First | PASS | Read-only Health Connect access. Zero network calls. No cloud sync. No health data persisted beyond in-memory cache for current session. |
| II. Pragmatic Simplicity | PASS | Single Gradle module. No DI framework — manual wiring via Application class. Charting via Vico (simpler than custom Canvas). No unnecessary abstractions. |
| III. Performance-Conscious Data Handling | PASS | All Health Connect reads via coroutines on `Dispatchers.IO`. Aggregates fetched per-session and cached in ViewModel. Pagination via page tokens for large result sets. |
| IV. Focused Testing | PASS | Unit tests target: pace calculation, split computation, stats aggregation, time-of-day derivation. No composable UI tests. |
| V. Future-Ready Data Boundaries | PASS | Domain models (`RunSession`, `RunDetail`) are separate from Health Connect record types. Mapping layer in `data.health` package. Health Connect types never imported in `ui` or `domain` packages. |

**Gate result**: ALL PASS — proceed to Phase 0.

## Post-Design Constitution Re-Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Privacy & Offline-First | PASS | No changes from initial check. Data flow is HC -> domain model -> UI, no persistence layer. |
| II. Pragmatic Simplicity | PASS | Architecture is 3 packages (data/domain/ui) in a single module. Manual DI via `CurrereApp`. ViewModels created with factory lambda. |
| III. Performance-Conscious Data Handling | PASS | Aggregate API combines distance+steps+duration+avgHR in single call per session. Results cached in ViewModel StateFlow. Detail data loaded on-demand. |
| IV. Focused Testing | PASS | Test scope limited to `domain.compute` package: `PaceCalculator`, `SplitCalculator`, `StatsAggregator`, `TimeOfDay`. |
| V. Future-Ready Data Boundaries | PASS | `data.health.HealthConnectSource` returns domain models. If a REST backend is added later, a new `data.api` package implements the same mapping pattern. |

**Post-design gate result**: ALL PASS.

## Project Structure

### Documentation (this feature)

```text
specs/001-run-diary/
├── plan.md              # This file
├── research.md          # Phase 0: research findings
├── data-model.md        # Phase 1: entity definitions
├── quickstart.md        # Phase 1: developer setup guide
└── tasks.md             # Created by /speckit.tasks (not this command)
```

### Source Code (repository root)

```text
app/src/main/java/nl/teunk/currere/
├── CurrereApp.kt                       # Application class (manual DI root)
├── MainActivity.kt                     # Single activity, hosts NavHost
├── data/
│   └── health/
│       ├── HealthConnectSource.kt       # Health Connect read operations
│       └── Mappers.kt                   # HC record types -> domain models
├── domain/
│   ├── model/
│   │   ├── RunSession.kt               # List-level model (summary stats)
│   │   ├── RunDetail.kt                # Detail-level model (charts + splits)
│   │   ├── HeartRateSample.kt          # Timestamped HR reading
│   │   ├── PaceSample.kt              # Timestamped pace reading
│   │   ├── PaceSplit.kt               # Per-km split data
│   │   └── TimeOfDay.kt               # Morning/Afternoon/Evening/Night enum
│   └── compute/
│       ├── PaceCalculator.kt           # Pace from speed, avg pace
│       ├── SplitCalculator.kt          # Per-km splits from distance records
│       └── StatsAggregator.kt          # Format durations, derive time-of-day
└── ui/
    ├── theme/
    │   ├── Theme.kt                    # Material 3 theme (light + dark)
    │   ├── Color.kt                    # Color palette
    │   └── Type.kt                     # Typography scale
    ├── navigation/
    │   ├── CurrereNavGraph.kt          # NavHost with routes
    │   └── Routes.kt                   # @Serializable route definitions
    ├── diary/
    │   ├── DiaryScreen.kt              # Run list with pull-to-refresh
    │   ├── DiaryViewModel.kt           # Loads sessions + cached summaries
    │   └── RunCard.kt                  # Single run card composable
    ├── detail/
    │   ├── DetailScreen.kt             # Scrollable detail layout
    │   ├── DetailViewModel.kt          # Loads detail data on-demand
    │   ├── StatsRow.kt                 # Summary stats with icons
    │   ├── HeartRateChart.kt           # Vico line chart for HR
    │   ├── PaceChart.kt               # Vico line chart for pace
    │   └── SplitsTable.kt             # Per-km splits with colored bars
    ├── permission/
    │   └── PermissionScreen.kt         # Permission request + denied state
    └── components/
        └── EmptyState.kt              # Reusable empty state composable

app/src/test/java/nl/teunk/currere/
└── domain/
    └── compute/
        ├── PaceCalculatorTest.kt
        ├── SplitCalculatorTest.kt
        └── StatsAggregatorTest.kt
```

**Structure Decision**: Single-module Android app with package-level separation into `data`/`domain`/`ui` layers. This follows Constitution Principle II (no multi-module Gradle setup) while maintaining Principle V (clean separation so a backend can be added later without rewriting domain or UI code).

### Contracts

Skipped. Currere is a standalone mobile app that consumes Health Connect data and presents it to the user. It does not expose APIs, CLIs, SDKs, or other interfaces to external systems.

## Complexity Tracking

No violations — table intentionally empty.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| *(none)*  |            |                                      |
