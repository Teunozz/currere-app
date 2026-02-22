# Tasks: Run Diary

**Input**: Design documents from `/specs/001-run-diary/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Included — Constitution Principle IV requires unit tests for domain and business logic (pace calculation, split computation, aggregations, time-of-day derivation).

**Organization**: Tasks grouped by user story. All three user stories are P1 but ordered by implementation dependency: US3 (Permissions) → US1 (Diary List) → US2 (Detail Screen).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- All file paths relative to repository root

---

## Phase 1: Setup (Project Configuration)

**Purpose**: Configure Gradle build system, add all dependencies, and prepare AndroidManifest for Health Connect integration.

- [x] T001 Update version catalog with Compose BOM, Health Connect, Vico, Navigation Compose, Lifecycle, Activity Compose, and Kotlin Serialization dependencies in gradle/libs.versions.toml
- [x] T002 Add kotlin-compose and kotlin-serialization plugin declarations to root build.gradle.kts
- [x] T003 Configure Compose buildFeatures, apply kotlin-compose and kotlin-serialization plugins, replace View-based dependencies with Compose and all new library dependencies in app/build.gradle.kts
- [x] T004 Add Health Connect read permissions (READ_EXERCISE, READ_DISTANCE, READ_STEPS, READ_HEART_RATE, READ_SPEED, READ_EXERCISE_ROUTE, READ_HEALTH_DATA_HISTORY), activity-alias for ViewPermissionUsageActivity, CurrereApp as android:name on Application, and MainActivity activity declaration in app/src/main/AndroidManifest.xml

**Checkpoint**: Project builds successfully with `./gradlew assembleDebug` — no source code yet, just configuration.

---

## Phase 2: Foundational (Domain Layer, Data Layer, App Scaffold)

**Purpose**: Build the complete domain model, computation logic with unit tests, Health Connect data layer, and app navigation scaffold. All user stories depend on this phase.

**CRITICAL**: No user story work can begin until this phase is complete.

### Domain Models

- [x] T005 [P] Create all domain model data classes: RunSession (id, startTime, endTime, distanceMeters, activeDuration, averagePaceSecondsPerKm, averageHeartRateBpm, title), RunDetail (session, totalSteps, heartRateSamples, paceSamples, splits), HeartRateSample (time, bpm), PaceSample (time, secondsPerKm), PaceSplit (kilometerNumber, distanceMeters, splitDuration, splitPaceSecondsPerKm, cumulativeDuration, isPartial), and TimeOfDay enum (MORNING/AFTERNOON/EVENING/NIGHT with hour ranges and labels) in app/src/main/java/nl/teunk/currere/domain/model/

### Computation Logic

- [x] T006 [P] Implement PaceCalculator with speed-to-pace conversion (1000.0 / speed_m_s for seconds/km), average pace from total duration and distance, and stationary speed filtering (skip 0 m/s samples) in app/src/main/java/nl/teunk/currere/domain/compute/PaceCalculator.kt
- [x] T007 [P] Implement SplitCalculator that takes sorted incremental distance segments (startTime, endTime, distanceMeters) and computes per-km splits by accumulating distance, interpolating km boundary crossing times, and handling the final partial split in app/src/main/java/nl/teunk/currere/domain/compute/SplitCalculator.kt
- [x] T008 [P] Implement StatsAggregator with duration formatting (h:mm:ss or mm:ss), distance formatting (km with 2 decimals), pace formatting (m:ss/km), and TimeOfDay derivation from Instant using device timezone (05:00–11:59 morning, 12:00–16:59 afternoon, 17:00–20:59 evening, 21:00–04:59 night) in app/src/main/java/nl/teunk/currere/domain/compute/StatsAggregator.kt

### Unit Tests

- [x] T009 [P] Write unit tests for PaceCalculator covering: normal speed-to-pace conversion, zero speed handling, average pace from duration and distance, null pace when distance is zero, and multiple speed samples in app/src/test/java/nl/teunk/currere/domain/compute/PaceCalculatorTest.kt
- [x] T010 [P] Write unit tests for SplitCalculator covering: exact 5km run (5 full splits), 15.01km run (15 full + 1 partial), sub-1km run (1 partial split), exact km boundary (no partial), sparse distance records with interpolation, and invariant that split distances sum to total distance in app/src/test/java/nl/teunk/currere/domain/compute/SplitCalculatorTest.kt
- [x] T011 [P] Write unit tests for StatsAggregator covering: time-of-day derivation for all four brackets including boundary hours (04:59→night, 05:00→morning, 11:59→morning, 12:00→afternoon), duration formatting (59:30 as mm:ss, 1:05:30 as h:mm:ss), distance formatting (15.01234→"15.01"), and pace formatting (300s/km→"5:00") in app/src/test/java/nl/teunk/currere/domain/compute/StatsAggregatorTest.kt

### Data Layer

- [x] T012 Implement DTO-to-domain mappers: ExerciseSessionRecord + AggregateResult → RunSession, HeartRateRecord.Sample → HeartRateSample, SpeedRecord.Sample → PaceSample (1000.0/speed conversion), DistanceRecord → SplitCalculator input, with validation (clamp negative distance, null pace for zero distance) in app/src/main/java/nl/teunk/currere/data/health/Mappers.kt
- [x] T013 Implement HealthConnectSource with: readRunningSessions() using paginated readRecords filtered by EXERCISE_TYPE_RUNNING, aggregateSessionStats() combining DISTANCE_TOTAL + COUNT_TOTAL + EXERCISE_DURATION_TOTAL + BPM_AVG in single aggregate call, readHeartRateSamples(), readSpeedSamples(), readDistanceRecords() for a session time range, all using Dispatchers.IO in app/src/main/java/nl/teunk/currere/data/health/HealthConnectSource.kt

### App Scaffold

- [x] T014 [P] Create Material 3 theme with CurrereTheme composable, light and dark color schemes, and Typography scale in app/src/main/java/nl/teunk/currere/ui/theme/Theme.kt, Color.kt, and Type.kt
- [x] T015 [P] Create @Serializable navigation route definitions: PermissionRoute (object), DiaryRoute (object), DetailRoute (data class with sessionId: String, startTimeEpochMilli: Long, endTimeEpochMilli: Long) in app/src/main/java/nl/teunk/currere/ui/navigation/Routes.kt
- [x] T016 Create CurrereApp Application class with lazy-initialized HealthConnectSource using HealthConnectClient.getOrCreate(this) in app/src/main/java/nl/teunk/currere/CurrereApp.kt
- [x] T017 Create CurrereNavGraph composable with NavHost containing PermissionRoute, DiaryRoute, and DetailRoute destinations (screen composables as stubs initially), and MainActivity that sets CurrereTheme with CurrereNavGraph as content in app/src/main/java/nl/teunk/currere/ui/navigation/CurrereNavGraph.kt and app/src/main/java/nl/teunk/currere/MainActivity.kt

**Checkpoint**: App builds, launches, and shows a stub screen. Unit tests pass with `./gradlew test`. Domain layer is complete and verified.

---

## Phase 3: User Story 3 — Grant Health Connect Permissions (Priority: P1)

**Goal**: First-time users are prompted for Health Connect read permissions. Denied users see an explanation with a button to open HC settings.

**Independent Test**: Install app fresh, open it, verify permission prompt appears. Grant → navigates to diary. Deny → shows explanation + settings button.

### Implementation for User Story 3

- [x] T018 [US3] Implement PermissionScreen composable with: rememberLauncherForActivityResult using PermissionController.createRequestPermissionResultContract(), permission request for READ_EXERCISE + READ_DISTANCE + READ_STEPS + READ_HEART_RATE + READ_SPEED + READ_EXERCISE_ROUTE + READ_HEALTH_DATA_HISTORY, denied state showing explanation text and "Open Health Connect settings" button using Health Connect's ACTION_HEALTH_CONNECT_SETTINGS intent, and onPermissionsGranted callback in app/src/main/java/nl/teunk/currere/ui/permission/PermissionScreen.kt
- [x] T019 [US3] Wire PermissionScreen into CurrereNavGraph: on app start check permissions via HealthConnectClient.permissionController.getGrantedPermissions(), navigate to DiaryRoute if granted or PermissionRoute if not, handle onPermissionsGranted by navigating to DiaryRoute with popUpTo removing PermissionRoute from backstack in app/src/main/java/nl/teunk/currere/ui/navigation/CurrereNavGraph.kt

**Checkpoint**: Fresh install shows permission prompt. Granting navigates to (stub) diary. Denying shows explanation with settings button.

---

## Phase 4: User Story 1 — Browse Run History (Priority: P1) MVP

**Goal**: Users see a scrollable list of all recorded runs ordered most recent first, each showing date/time, distance, exercise time, and average pace. Pull-to-refresh re-queries Health Connect. Empty state shown when no runs exist.

**Independent Test**: Open app with Health Connect data present. Run list displays correctly with accurate stats. Pull-to-refresh works. No runs shows empty state.

### Implementation for User Story 1

- [x] T020 [US1] Implement DiaryViewModel taking HealthConnectSource, with: StateFlow<DiaryUiState> (Loading, Empty, Success with List<RunSession>), loadSessions() reading all running sessions then aggregating stats per session with caching, refresh() for pull-to-refresh re-query, all Health Connect reads on viewModelScope + Dispatchers.IO in app/src/main/java/nl/teunk/currere/ui/diary/DiaryViewModel.kt
- [x] T021 [P] [US1] Create RunCard composable displaying: formatted date/time (e.g. "Sat 21 Jun 2025, 07:32"), distance in km to 2 decimals, exercise time as h:mm:ss or mm:ss, average pace as m:ss/km, and activity title derived from TimeOfDay — using StatsAggregator formatting functions, with onClick callback for navigation in app/src/main/java/nl/teunk/currere/ui/diary/RunCard.kt
- [x] T022 [P] [US1] Create EmptyState composable with icon, explanatory text that the app reads running data from Health Connect, and suggestion to record runs with the user's preferred fitness app in app/src/main/java/nl/teunk/currere/ui/components/EmptyState.kt
- [x] T023 [US1] Implement DiaryScreen composable with: LazyColumn of RunCard items, pullToRefresh modifier triggering DiaryViewModel.refresh(), EmptyState shown when no sessions, loading indicator during initial load, and onRunClick callback navigating to DetailRoute with session ID and time range in app/src/main/java/nl/teunk/currere/ui/diary/DiaryScreen.kt
- [x] T024 [US1] Replace DiaryRoute stub in CurrereNavGraph with DiaryScreen, create DiaryViewModel via viewModel factory using CurrereApp.healthConnectSource, and wire onRunClick to navigate to DetailRoute in app/src/main/java/nl/teunk/currere/ui/navigation/CurrereNavGraph.kt

**Checkpoint**: App shows run list with real Health Connect data. Pull-to-refresh works. Empty state shows when no runs. Tapping a card navigates (to stub detail for now).

---

## Phase 5: User Story 2 — View Run Details (Priority: P1)

**Goal**: Tapping a run card shows a detailed breakdown with summary stats (distance, time, HR, pace, steps), heart rate line chart, pace line chart, and per-km pace splits table with colored speed bars.

**Independent Test**: Tap any run card. Detail screen loads with correct stats, functional charts with reference lines, and accurate pace splits including partial final split.

### Implementation for User Story 2

- [x] T025 [US2] Implement DetailViewModel taking HealthConnectSource, sessionId, startTime, and endTime, with: StateFlow<DetailUiState> (Loading, Success with RunDetail), parallel loading of heart rate samples, speed samples, distance records, and aggregate stats on Dispatchers.IO, then computing PaceSamples via PaceCalculator and PaceSplits via SplitCalculator on Dispatchers.Default in app/src/main/java/nl/teunk/currere/ui/detail/DetailViewModel.kt
- [x] T026 [P] [US2] Create StatsRow composable displaying a row of stat items: distance (km) with ruler icon, exercise time (min) with clock icon, average heart rate (bpm) with heart icon or "—" if null, average pace (min:ss/km) with speed icon, total steps with footprints icon — each using Material 3 styling with label and value in app/src/main/java/nl/teunk/currere/ui/detail/StatsRow.kt
- [x] T027 [P] [US2] Create HeartRateChart composable using Vico CartesianChartHost with: Line layer plotting bpm over elapsed time, y-axis bounded to min/max HR values, horizontal reference line at average HR with label, time-based x-axis labels, Material 3 themed colors, and semantics contentDescription summarizing min/avg/max HR for accessibility in app/src/main/java/nl/teunk/currere/ui/detail/HeartRateChart.kt
- [x] T028 [P] [US2] Create PaceChart composable using Vico CartesianChartHost with: Line layer plotting pace (min:ss/km) over elapsed time, y-axis bounded to min/max pace (inverted — slower pace higher), horizontal reference line at average pace with label, time-based x-axis labels, and semantics contentDescription for accessibility in app/src/main/java/nl/teunk/currere/ui/detail/PaceChart.kt
- [x] T029 [P] [US2] Create SplitsTable composable displaying per-km rows: km number, split pace (m:ss), colored horizontal bar (width proportional to pace — longer = slower, color gradient from green/fast to red/slow relative to the run's pace range), cumulative active time, and "partial" label on final row if isPartial is true in app/src/main/java/nl/teunk/currere/ui/detail/SplitsTable.kt
- [x] T030 [US2] Implement DetailScreen composable with: vertically scrollable Column, header showing activity title + date + time range, StatsRow, conditional HeartRateChart (hidden when heartRateSamples is empty), PaceChart, SplitsTable, loading indicator during data load, and back navigation in app/src/main/java/nl/teunk/currere/ui/detail/DetailScreen.kt
- [x] T031 [US2] Replace DetailRoute stub in CurrereNavGraph with DetailScreen, create DetailViewModel via viewModel factory passing HealthConnectSource and route arguments (sessionId, startTimeEpochMilli, endTimeEpochMilli) in app/src/main/java/nl/teunk/currere/ui/navigation/CurrereNavGraph.kt

**Checkpoint**: Full detail screen works — stats, charts with reference lines, pace splits with colored bars. HR chart hidden when no HR data. Partial final split row shown correctly.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Edge case handling, accessibility, cleanup, and final validation.

- [x] T032 Add semantics contentDescription to all interactive elements (RunCard, EmptyState button, permission button) and verify chart text alternatives describe data trends (min, avg, max values) across all composables in app/src/main/java/nl/teunk/currere/ui/
- [x] T033 Handle revoked permissions: add permission re-check in CurrereNavGraph on resume (Lifecycle ON_RESUME), navigate to PermissionRoute if permissions lost, covering the edge case where user revokes permissions via system settings while app is backgrounded in app/src/main/java/nl/teunk/currere/ui/navigation/CurrereNavGraph.kt
- [x] T034 Remove unused View-based resource files (app/src/main/res/values/themes.xml, app/src/main/res/values/colors.xml, app/src/main/res/values-night/themes.xml) and example test stubs (ExampleUnitTest.kt, ExampleInstrumentedTest.kt), update app/src/main/res/values/strings.xml with app-level string resources
- [x] T035 Run quickstart.md validation: verify `./gradlew assembleDebug` succeeds, `./gradlew test` passes all unit tests, and manually walk through all 6 test scenarios from quickstart.md (run list, empty state, detail screen, no heart rate, pull to refresh, permission denied)

**Checkpoint**: All edge cases handled. Accessibility verified. Build and tests green. Manual test scenarios pass.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories
- **US3 Permissions (Phase 3)**: Depends on Phase 2 (needs CurrereNavGraph, theme, HealthConnectClient for permission check)
- **US1 Diary (Phase 4)**: Depends on Phase 2 (needs HealthConnectSource, domain models, StatsAggregator)
- **US2 Detail (Phase 5)**: Depends on Phase 2 (needs HealthConnectSource, all domain models, PaceCalculator, SplitCalculator). Also benefits from US1 (navigation wiring) but is independently testable.
- **Polish (Phase 6)**: Depends on Phases 3, 4, 5 all being complete

### User Story Dependencies

- **US3 (Permissions)**: Can start after Phase 2 — no dependency on US1 or US2
- **US1 (Diary List)**: Can start after Phase 2 — no dependency on US3 or US2
- **US2 (Detail Screen)**: Can start after Phase 2 — no dependency on US3 or US1 (but navigation from list→detail is wired in T031)

All three user stories can proceed in parallel after Phase 2, but sequential order (US3 → US1 → US2) is recommended for a single developer as it follows the natural user flow and builds incrementally.

### Within Phase 2

```
T005 (models) ──┬──→ T012 (mappers) → T013 (HC source) → T016 (CurrereApp)
                │
T006 (pace)   ──┤
T007 (splits) ──┤    T009 (pace test)
T008 (stats)  ──┤    T010 (split test)
                │    T011 (stats test)
T014 (theme)  ──┤
T015 (routes) ──┴──→ T017 (NavGraph + MainActivity)
```

### Parallel Opportunities

**Phase 2 — Wave 1** (no dependencies):
```
T005 (domain models)  |  T006 (PaceCalculator)  |  T007 (SplitCalculator)
T008 (StatsAggregator)  |  T014 (theme)  |  T015 (routes)
```

**Phase 2 — Wave 2** (after Wave 1):
```
T009 (pace test)  |  T010 (split test)  |  T011 (stats test)
T012 (mappers)  |  T016 (CurrereApp)  |  T017 (NavGraph + MainActivity)
```

**Phase 2 — Wave 3** (after Wave 2):
```
T013 (HealthConnectSource — depends on T012 mappers)
```

**Phase 4 — US1 parallel**:
```
T021 (RunCard)  |  T022 (EmptyState)
```

**Phase 5 — US2 parallel**:
```
T026 (StatsRow)  |  T027 (HeartRateChart)  |  T028 (PaceChart)  |  T029 (SplitsTable)
```

---

## Parallel Example: User Story 2

```bash
# After T025 (DetailViewModel) is complete, launch all UI components in parallel:
Task: "Create StatsRow in app/.../ui/detail/StatsRow.kt"
Task: "Create HeartRateChart in app/.../ui/detail/HeartRateChart.kt"
Task: "Create PaceChart in app/.../ui/detail/PaceChart.kt"
Task: "Create SplitsTable in app/.../ui/detail/SplitsTable.kt"

# Then assemble them:
Task: "Create DetailScreen in app/.../ui/detail/DetailScreen.kt"
Task: "Wire DetailScreen into CurrereNavGraph"
```

---

## Implementation Strategy

### MVP First (Phases 1–4: Setup + Foundational + US3 + US1)

1. Complete Phase 1: Setup → Project builds
2. Complete Phase 2: Foundational → Domain layer tested, data layer ready
3. Complete Phase 3: US3 → Permission flow works
4. Complete Phase 4: US1 → **STOP and VALIDATE**: Run list displays real data, pull-to-refresh works, empty state shows
5. This is the MVP — a functional run diary app without detail screen

### Full Feature (add Phase 5: US2)

6. Complete Phase 5: US2 → Detail screen with charts and splits
7. Complete Phase 6: Polish → Edge cases, accessibility, cleanup
8. **Feature complete** — all acceptance scenarios from spec.md are met

### Incremental Delivery

1. Setup + Foundational → Tested domain logic, compilable app
2. Add US3 (Permissions) → Testable permission flow
3. Add US1 (Diary List) → **MVP** — testable run list with real data
4. Add US2 (Detail Screen) → Full feature — charts, splits, complete detail view
5. Polish → Production-ready with accessibility and edge case handling

---

## Notes

- [P] tasks = different files, no dependencies on other [P] tasks in the same wave
- [Story] label maps task to specific user story for traceability
- All file paths use `app/src/main/java/nl/teunk/currere/` as the source root
- Test files use `app/src/test/java/nl/teunk/currere/` as the test root
- Commit after each phase checkpoint for clean rollback points
- Vico library coordinates should be verified at implementation time (check latest 2.x release)
