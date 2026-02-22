# Per-split pace accuracy fix

## Status
Blocked — needs real Health Connect data sample to debug.

## Problem
The splits table shows the same pace (e.g. 5:12) for every row instead of the actual per-km pace. Samsung Health shows varying paces (e.g. 5:19, 5:06, 5:10, 5:03, 5:02) for the same run.

## Root cause
Health Connect `DistanceRecord` segments have uniform timing — the recording app distributes total time evenly across segments. So `splitDuration / 1km` yields the same value for every split.

## What was tried
1. **Averaging PaceSample values per split window** — Incorrect because: (a) averaging pace (reciprocal of speed) is mathematically biased, and (b) the split time windows from distance segments are wrong (uniform), so we average the wrong samples per split.
2. **Trapezoidal integration of SpeedRecord samples** — Integrate speed (m/s) over time to find actual km boundaries, then derive pace from the real split durations. Results still didn't match Samsung Health (e.g. got 5:30, 4:51, 5:01 vs expected 5:19, 5:06, 5:10).

## Next steps
- Add debug logging to dump raw `DistanceRecord` and `SpeedRecord` data for a real run session (timestamps, values, data origins) so we can see exactly what Health Connect provides.
- Compare the raw data against Samsung Health's split values to understand how they compute per-km pace.
- Likely need to understand whether Samsung Health uses its own internal data (not exposed via Health Connect) or a different algorithm.

## Relevant files
- `SplitCalculator.kt` — split computation logic
- `HealthConnectSource.kt` — fetches DistanceRecord and SpeedRecord
- `Mappers.kt` — converts Health Connect records to domain models
- `SplitsTable.kt` — UI display
