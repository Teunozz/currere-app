# Data Model: Run Diary

## Domain Models

These models live in `domain.model` and are the only types used by the UI layer. They have no dependency on Health Connect SDK types.

### RunSession

Represents a single run in the diary list. Contains pre-computed summary stats.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Health Connect session UID |
| `startTime` | `Instant` | Session start timestamp |
| `endTime` | `Instant` | Session end timestamp |
| `distanceMeters` | `Double` | Total distance in meters |
| `activeDuration` | `Duration` | Active exercise time (excludes pauses) |
| `averagePaceSecondsPerKm` | `Double?` | Average pace in seconds per km (null if distance is 0) |
| `averageHeartRateBpm` | `Long?` | Average heart rate (null if no HR data) |
| `title` | `String` | Derived from time of day (e.g. "Morning run") |

**Computed display values** (in UI layer, not stored):
- `distanceKm`: `distanceMeters / 1000.0`, formatted to 2 decimals
- `formattedDuration`: `h:mm:ss` or `mm:ss` depending on length
- `formattedPace`: `m:ss/km`

### RunDetail

Represents the full data for a single run detail screen. Extends `RunSession` with chart and split data.

| Field | Type | Description |
|-------|------|-------------|
| `session` | `RunSession` | The base session summary |
| `totalSteps` | `Long` | Total step count |
| `heartRateSamples` | `List<HeartRateSample>` | Time-series HR data (empty if no HR) |
| `paceSamples` | `List<PaceSample>` | Time-series pace data |
| `splits` | `List<PaceSplit>` | Per-km split breakdown |

### HeartRateSample

A single heart rate reading within a session.

| Field | Type | Description |
|-------|------|-------------|
| `time` | `Instant` | Timestamp of reading |
| `bpm` | `Long` | Beats per minute |

### PaceSample

A single pace reading within a session, derived from speed records.

| Field | Type | Description |
|-------|------|-------------|
| `time` | `Instant` | Timestamp of reading |
| `secondsPerKm` | `Double` | Pace in seconds per kilometer |

### PaceSplit

A per-kilometer breakdown row for the splits table.

| Field | Type | Description |
|-------|------|-------------|
| `kilometerNumber` | `Int` | 1-indexed km number |
| `distanceMeters` | `Double` | Distance of this split (1000.0 for full, <1000.0 for final partial) |
| `splitDuration` | `Duration` | Time to cover this split |
| `splitPaceSecondsPerKm` | `Double` | Pace for this split in seconds/km |
| `cumulativeDuration` | `Duration` | Total active time at end of this split |
| `isPartial` | `Boolean` | True for the final partial-km split |

### TimeOfDay

Enum for activity title derivation.

| Value | Hour Range | Label |
|-------|-----------|-------|
| `MORNING` | 05:00–11:59 | "Morning run" |
| `AFTERNOON` | 12:00–16:59 | "Afternoon run" |
| `EVENING` | 17:00–20:59 | "Evening run" |
| `NIGHT` | 21:00–04:59 | "Night run" |

## Relationships

```
RunSession (1) ──── (1) RunDetail
                         ├── (0..*) HeartRateSample
                         ├── (0..*) PaceSample
                         └── (1..*) PaceSplit
```

- A `RunSession` always exists for a listed run
- A `RunDetail` is loaded on-demand when the user taps a run card
- `HeartRateSample` list may be empty (no HR sensor)
- `PaceSample` list may be empty (no speed data recorded)
- `PaceSplit` list always has at least one entry (partial split for any non-zero distance run)

## Health Connect DTO Mapping

The `data.health.Mappers` module converts Health Connect SDK types to domain models. This mapping is the **only place** where Health Connect types are referenced.

| Health Connect Type | Domain Model | Mapping Notes |
|----|----|----|
| `ExerciseSessionRecord` | `RunSession` | Extract `startTime`, `endTime`, `metadata.id`. Title derived from `startTime` via `TimeOfDay`. |
| `AggregateResult[DISTANCE_TOTAL]` | `RunSession.distanceMeters` | `Length.inMeters` |
| `AggregateResult[EXERCISE_DURATION_TOTAL]` | `RunSession.activeDuration` | Direct `Duration` mapping |
| `AggregateResult[BPM_AVG]` | `RunSession.averageHeartRateBpm` | Nullable — may not exist |
| `AggregateResult[COUNT_TOTAL]` | `RunDetail.totalSteps` | `Long` |
| `HeartRateRecord.Sample` | `HeartRateSample` | `time` + `beatsPerMinute` |
| `SpeedRecord.Sample` | `PaceSample` | Convert `speed.inMetersPerSecond` to `1000.0 / speed` for seconds/km |
| `DistanceRecord` | Input to `SplitCalculator` | Incremental distance segments used to compute `PaceSplit` list |

## Validation Rules

| Rule | Where Enforced | Behavior |
|------|----------------|----------|
| `distanceMeters >= 0` | Mapper | Clamp negative values to 0 |
| `activeDuration > 0` | Mapper | Sessions with zero duration are still displayed |
| `averagePaceSecondsPerKm` is null when distance is 0 | Mapper | Avoids division by zero |
| `bpm` in range 30–250 | Not enforced | Trust Health Connect data; display as-is |
| `secondsPerKm > 0` | `PaceCalculator` | Skip speed samples with 0 m/s (stationary) |
| Splits sum to total distance | `SplitCalculator` | Invariant — verified in unit tests |

## State Transitions

Runs are read-only and have no mutable state within the app. The only relevant state is the **UI loading state**:

```
App Launch → [Permission Check]
  ├── Granted → Load Sessions → Display Diary
  │                                └── Tap Card → Load Detail → Display Detail
  └── Denied → Show Permission Explanation
                  └── Tap Settings → Open HC Settings
```
