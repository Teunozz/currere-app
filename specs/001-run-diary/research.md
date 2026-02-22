# Research: Run Diary

## R1: Health Connect SDK for Android 16 (SDK 36)

**Decision**: Use `androidx.health.connect:connect-client:1.1.0` (stable).

**Rationale**: On SDK 36, Health Connect is a platform component (integrated since Android 14). The connect-client library provides the Kotlin API surface. Version 1.1.0 is the latest stable release and supports all required record types. No alpha features are needed.

**Alternatives considered**:
- `1.2.0-alpha02`: Offers background read and new data types. Not needed — Currere only reads in the foreground and uses standard record types.

### Key API patterns

**Reading sessions** — filter by exercise type:
```kotlin
val response = healthConnectClient.readRecords(
    ReadRecordsRequest(
        ExerciseSessionRecord::class,
        timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH)
    )
)
// Filter for running: EXERCISE_TYPE_RUNNING (56)
val runs = response.records.filter {
    it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
}
```

**Aggregating per-session stats** — single call for multiple metrics:
```kotlin
val agg = healthConnectClient.aggregate(
    AggregateRequest(
        metrics = setOf(
            DistanceRecord.DISTANCE_TOTAL,
            StepsRecord.COUNT_TOTAL,
            ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
            HeartRateRecord.BPM_AVG
        ),
        timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
    )
)
val distance = agg[DistanceRecord.DISTANCE_TOTAL] // Length (meters)
val steps = agg[StepsRecord.COUNT_TOTAL]           // Long
val duration = agg[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL] // Duration
val avgHr = agg[HeartRateRecord.BPM_AVG]           // Long
```

**Reading time-series data** (heart rate, speed):
```kotlin
val hrRecords = healthConnectClient.readRecords(
    ReadRecordsRequest(
        HeartRateRecord::class,
        timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
    )
)
// Flatten: each HeartRateRecord contains multiple samples
val samples = hrRecords.records.flatMap { it.samples }
```

**Pagination** — use `pageToken` for large result sets:
```kotlin
var pageToken: String? = null
val allRecords = mutableListOf<ExerciseSessionRecord>()
do {
    val response = healthConnectClient.readRecords(
        ReadRecordsRequest(
            ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
            pageToken = pageToken
        )
    )
    allRecords.addAll(response.records)
    pageToken = response.pageToken
} while (pageToken != null)
```

**Permissions** — required read permissions for this feature:
- `android.permission.health.READ_EXERCISE`
- `android.permission.health.READ_DISTANCE`
- `android.permission.health.READ_STEPS`
- `android.permission.health.READ_HEART_RATE`
- `android.permission.health.READ_SPEED`
- `android.permission.health.READ_EXERCISE_ROUTE` (requested but unused — reserved for future map)

The `READ_HEALTH_DATA_HISTORY` permission is needed to read data older than 30 days. Should be requested as well.

### Exercise type mapping

The spec mentions both RUNNING and JOGGING session types. Health Connect defines `EXERCISE_TYPE_RUNNING` (56). There is no separate `EXERCISE_TYPE_JOGGING` constant. Some fitness apps may record jogs using `EXERCISE_TYPE_RUNNING` regardless of pace. The implementation should filter for `EXERCISE_TYPE_RUNNING` only. If a JOGGING type is later added to the SDK, the filter can be expanded.

### Exercise time (active duration)

The spec requires "active moving time only." The aggregate metric `ExerciseSessionRecord.EXERCISE_DURATION_TOTAL` returns total active exercise duration, excluding pauses. This is the correct metric to use.

---

## R2: Charting Library

**Decision**: Use Vico 2.x.

**Rationale**: Vico is the most mature Compose-native charting library. It supports:
- Line charts with reference lines (needed for avg HR and avg pace lines)
- Customizable axis bounds (min/max on y-axis)
- Custom styling and colors
- Smooth animations
- Good documentation and active maintenance

The two charts needed (heart rate over time, pace over time) are straightforward Vico line charts. The pace splits visualization (horizontal colored bars) will be built with Compose `Canvas`/`Row` — simpler than configuring a bar chart library for a non-standard layout.

**Alternatives considered**:
- **Y-Charts**: Lighter weight, but weaker reference line support. Vico's reference line API directly maps to the avg HR/avg pace requirement.
- **Custom Canvas**: More control, but significantly more code for axis labels, scaling, touch handling. Violates Pragmatic Simplicity for charts.
- **MPAndroidChart**: View-based, not Compose-native. Would require `AndroidView` wrapper.

---

## R3: Navigation

**Decision**: Type-safe Navigation Compose with Kotlin Serialization.

**Rationale**: Navigation 2.8+ supports `@Serializable` route classes, providing compile-time type safety for route arguments. This is the officially recommended approach for new Compose projects. The app has only 3 destinations (permission, diary, detail), so the navigation graph is trivial.

**Routes**:
```kotlin
@Serializable object PermissionRoute
@Serializable object DiaryRoute
@Serializable data class DetailRoute(val sessionId: String)
```

**Dependencies**:
- `androidx.navigation:navigation-compose:2.8.x`
- `org.jetbrains.kotlin.plugin.serialization` Gradle plugin
- `org.jetbrains.kotlinx:kotlinx-serialization-json`

**Alternatives considered**:
- **String-based routes**: Still works but loses type safety. No benefit for a new project.
- **Voyager/Decompose**: Third-party navigation libraries. Unnecessary complexity for 3 screens.

---

## R4: Compose + Material 3 Setup

**Decision**: Use Compose BOM for dependency alignment. Add `kotlin-compose` compiler plugin.

**Rationale**: Since Kotlin 2.0, the Compose compiler is a Kotlin compiler plugin (`org.jetbrains.kotlin.plugin.compose`). The Compose BOM ensures all Compose libraries are version-aligned.

**Dependencies to add to version catalog**:
```toml
[versions]
composeBom = "2025.01.01"
healthConnect = "1.1.0-alpha09"
vico = "2.1.2"
navigationCompose = "2.8.6"
kotlinxSerialization = "1.7.3"
activityCompose = "1.9.3"
lifecycleRuntimeCompose = "2.8.7"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeCompose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeCompose" }
health-connect = { group = "androidx.health.connect", name = "connect-client", version.ref = "healthConnect" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

[plugins]
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

Note: Vico dependency will be added via its own coordinates (check latest at implementation time).

**Alternatives considered**:
- **View-based UI**: The project skeleton uses the old Material/View theme. Constitution mandates Jetpack Compose + Material 3. The existing theme XML files will be replaced with Compose theming.

---

## R5: Architecture Without DI

**Decision**: Manual dependency wiring via `Application` class.

**Rationale**: Constitution Principle II prohibits DI frameworks. The app has exactly one data source (`HealthConnectSource`) and a handful of pure computation functions. Manual wiring is trivial.

**Pattern**:
```kotlin
class CurrereApp : Application() {
    val healthConnectSource: HealthConnectSource by lazy {
        HealthConnectSource(HealthConnectClient.getOrCreate(this))
    }
}
```

ViewModels access the source via a factory:
```kotlin
// In DiaryScreen.kt
val viewModel: DiaryViewModel = viewModel {
    val app = (LocalContext.current.applicationContext as CurrereApp)
    DiaryViewModel(app.healthConnectSource)
}
```

Computation functions in `domain.compute` are pure (no dependencies) — called directly from ViewModels.

**Alternatives considered**:
- **Hilt/Dagger**: Prohibited by constitution.
- **Koin**: Lighter weight DI, but still a framework. Unnecessary for 1 dependency.
- **Singleton object**: Less testable than `Application`-scoped lazy init.

---

## R6: Pace and Split Calculations

**Decision**: Compute pace from speed records; compute splits from distance records.

### Pace calculation

Speed records (`SpeedRecord`) contain timestamped speed samples in m/s. Convert to pace:
```
pace_seconds_per_km = 1000.0 / speed_meters_per_second
pace_minutes_per_km = pace_seconds_per_km / 60.0
```

Average pace for a run = total exercise duration / total distance.

### Split calculation

Distance records (`DistanceRecord`) represent incremental distance segments with `startTime`, `endTime`, and `distance`. To compute per-km splits:

1. Sort distance records by `startTime`
2. Walk through records, accumulating distance
3. When cumulative distance crosses a km boundary, interpolate the exact crossing time
4. Split pace = time elapsed for that km segment
5. Final partial split = remaining distance after last full km, with its elapsed time

Edge cases:
- Very short runs (<1 km): Single partial split
- Exact km distances: No partial split row
- Sparse distance records: Interpolate linearly between record boundaries

### Time-of-day derivation

From session start time (in device local timezone):
- 05:00–11:59 → "Morning run"
- 12:00–16:59 → "Afternoon run"
- 17:00–20:59 → "Evening run"
- 21:00–04:59 → "Night run"

---

## R7: Performance Strategy

**Decision**: Load sessions first, then aggregate stats progressively.

### Run list loading

1. Read all `ExerciseSessionRecord` (type RUNNING) — single paginated query
2. Sort by `startTime` descending
3. For each session, call `aggregate()` with combined metrics (distance, steps, duration, avg HR) — single call per session
4. Cache results in ViewModel `StateFlow<List<RunSession>>`
5. Show sessions immediately with a loading indicator per card until aggregates arrive

### Detail screen loading

1. On navigation, pass session start/end time (or session ID) as route argument
2. Load in parallel on IO dispatcher:
   - Heart rate samples (`HeartRateRecord.readRecords`)
   - Speed samples (`SpeedRecord.readRecords`)
   - Distance records (`DistanceRecord.readRecords`)
   - Aggregate stats (reuse from cache or re-fetch)
3. Compute splits from distance records (pure function, on Default dispatcher)
4. Compute pace samples from speed records
5. Emit `RunDetail` to UI via StateFlow

### Memory management

- No persistent cache — all data is in ViewModel scope
- When ViewModel is cleared (back navigation), data is GC'd
- For 500 sessions, cached summaries are ~50KB (negligible)
