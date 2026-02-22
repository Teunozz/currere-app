# Feature Specification: Run Diary

**Feature Branch**: `001-run-diary`
**Created**: 2026-02-22
**Status**: Draft
**Input**: User description: "Build Currere — a read-only Health Connect companion app with a run diary list and run detail screen"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse Run History (Priority: P1)

As a runner, I open Currere and see a scrollable list of all my recorded runs ordered by most recent first, so I can quickly review my running activity.

Each run card shows:
- Date and time (e.g. "Sat 21 Jun 2025, 07:32")
- Total distance (km, two decimals, e.g. "15.01 km")
- Total exercise time (h:mm:ss or mm:ss — active moving time only)
- Average pace (m:ss/km)

**Why this priority**: Without a list of runs, there is no app. This is the entry point for all user interaction and the minimum viable product.

**Independent Test**: Open the app with Health Connect data present. The run list displays correctly with accurate stats for each run.

**Acceptance Scenarios**:

1. **Given** the user has 10 running sessions in Health Connect, **When** they open Currere, **Then** they see 10 run cards ordered most recent first, each showing date/time, distance, duration, and average pace.
2. **Given** the user has no running sessions in Health Connect, **When** they open Currere, **Then** they see an empty state explaining that the app reads from Health Connect and suggesting they record runs with their preferred fitness app.
3. **Given** the run list is displayed, **When** the user pulls to refresh, **Then** the list re-queries Health Connect and reflects any new or updated sessions.
4. **Given** the user has runs recorded as both RUNNING and JOGGING session types, **When** they open Currere, **Then** both types appear in the list (no distinction made between them).

---

### User Story 2 - View Run Details (Priority: P1)

As a runner, I tap a run card to see a detailed breakdown of that run, including summary stats, heart rate chart, pace chart, and per-kilometer pace splits.

**Why this priority**: The detail screen is the core value proposition — presenting rich running data in a clean, focused view. Without it, the app is just a list of numbers.

**Independent Test**: Tap any run card. The detail screen loads with correct summary stats, functional charts, and accurate pace splits.

**Acceptance Scenarios**:

1. **Given** the user taps a run card, **When** the detail screen loads, **Then** they see a header with activity title (derived from time of day, e.g. "Morning run"), date, and time range.
2. **Given** the detail screen is loaded, **When** the user views the summary stats section, **Then** they see distance (km), exercise time (minutes), average heart rate (bpm), average pace (min:ss/km), and total steps — each with an icon and label.
3. **Given** heart rate data exists for the run, **When** the detail screen is loaded, **Then** a line chart shows heart rate over elapsed time, with min/max on the y-axis and an average HR reference line.
4. **Given** heart rate data does NOT exist for the run, **When** the detail screen is loaded, **Then** the heart rate chart section is hidden (not shown as empty/error).
5. **Given** the detail screen is loaded, **When** the user views the pace chart, **Then** a line chart shows pace (min:ss/km) over elapsed time, with min/max on the y-axis and an average pace reference line.
6. **Given** the run is 15.01 km, **When** the user views the pace splits table, **Then** they see 16 rows: km 1 through km 15 (full splits) plus the final 0.01 km partial split, each showing split pace (min:ss), a colored horizontal bar (longer = slower, color indicates relative speed), and cumulative active time.

---

### User Story 3 - Grant Health Connect Permissions (Priority: P1)

As a first-time user, I am prompted to grant Health Connect read permissions so the app can access my running data.

**Why this priority**: Without permissions, the app cannot display any data. This is a prerequisite for all other functionality.

**Independent Test**: Install the app fresh, open it, and verify the permission flow works for both grant and deny scenarios.

**Acceptance Scenarios**:

1. **Given** the user has never opened Currere before, **When** they launch the app, **Then** the app requests Health Connect read permissions for exercise sessions, distance, steps, heart rate, and exercise routes.
2. **Given** the user grants all requested permissions, **When** the permission flow completes, **Then** the app navigates to the run diary and loads data.
3. **Given** the user denies permissions, **When** the permission flow completes, **Then** the app shows a clear explanation of why permissions are needed and a button to open Health Connect settings.
4. **Given** the user previously denied permissions, **When** they tap the "Open Health Connect settings" button, **Then** the system Health Connect permission screen opens.

---

### Edge Cases

- What happens when a run has distance data but no heart rate data? The detail screen hides the heart rate chart and the heart rate summary stat shows "—" or is omitted.
- What happens when a run has zero steps recorded? The steps stat shows "0" — it is not hidden.
- What happens when a run is extremely short (e.g. 0.1 km, under 1 minute)? The app displays it normally with a single partial-km split row.
- What happens when a run is extremely long (e.g. 100 km ultra-marathon)? The splits table shows all 100+ rows; the list scrolls vertically.
- What happens when Health Connect returns sessions while a refresh is in progress? The refresh completes with the latest data; no duplicate sessions appear.
- What happens when the user revokes permissions after initially granting them? The app detects the missing permissions and shows the permission explanation screen.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST read exercise session data (type RUNNING and JOGGING) from Health Connect.
- **FR-002**: The app MUST NOT write any data to Health Connect — it is strictly read-only.
- **FR-003**: The run diary MUST display all running/jogging sessions ordered by most recent start time first.
- **FR-004**: Each run card MUST show: date/time, distance (km, 2 decimals), exercise time (h:mm:ss or mm:ss), and average pace (m:ss/km).
- **FR-005**: The run detail screen MUST show: activity title (time-of-day derived), date/time range, distance, exercise time, average heart rate, average pace, and total steps.
- **FR-006**: The run detail screen MUST include a heart rate line chart with min/max bounds and average reference line.
- **FR-007**: The run detail screen MUST include a pace line chart with min/max bounds and average pace reference line.
- **FR-008**: The run detail screen MUST include a pace splits table showing per-km split pace (with colored bar visualization), and cumulative time.
- **FR-009**: The run diary MUST support pull-to-refresh to re-query Health Connect.
- **FR-010**: The app MUST request Health Connect read permissions for: exercise sessions, distance, steps, heart rate, and exercise routes.
- **FR-011**: If permissions are denied, the app MUST show an explanation and a button to open Health Connect settings.
- **FR-012**: If no runs exist, the app MUST show an empty state explaining that it reads from Health Connect.
- **FR-013**: The app MUST NOT display calories, heart points, badges, streaks, encouragement messages, or any gamification elements.
- **FR-014**: If heart rate data is unavailable for a run, the heart rate chart MUST be hidden and the heart rate stat MUST show a placeholder or be omitted.
- **FR-015**: The pace splits table MUST include a final partial-km row when the run distance is not an exact number of kilometers.
- **FR-016**: Activity title MUST be derived from time of day (e.g. "Morning run", "Afternoon run", "Evening run").
- **FR-017**: All charts MUST have text alternatives for accessibility.

### Key Entities

- **Run Session**: A single running or jogging exercise session. Key attributes: start time, end time, session type (running/jogging).
- **Run Summary**: Computed aggregates for a session: total distance, exercise time, average pace, average heart rate, total steps.
- **Heart Rate Sample**: A timestamped heart rate reading (bpm) within a session.
- **Pace Sample**: A timestamped pace reading (min:ss/km) within a session, derived from distance records.
- **Pace Split**: A per-kilometer breakdown: km number, split pace, cumulative time, relative speed indicator.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can view their full run history within 2 seconds of opening the app (after initial permission grant).
- **SC-002**: Tapping a run card loads the complete detail screen (stats, charts, splits) within 1 second.
- **SC-003**: The run list scrolls at a consistent 60 fps with no dropped frames, even with 500+ sessions.
- **SC-004**: Pull-to-refresh completes and updates the list within 3 seconds.
- **SC-005**: All calculated values (pace, splits, averages) match Health Connect source data with no rounding or aggregation errors beyond display formatting.
- **SC-006**: All charts have text alternatives that convey the same information to screen reader users.
- **SC-007**: The app functions fully without network access — no loading spinners, no error screens, no degraded states.
- **SC-008**: The permission flow takes the user from first launch to seeing their run data in 3 taps or fewer.

### Assumptions

- The user has Health Connect installed and at least one other app (Garmin, Samsung Health, Google Fit, etc.) recording running data.
- All display units are metric (kilometers, min/km). Imperial units are out of scope for this feature.
- The app language is Dutch for user-facing labels where the user description used Dutch terms (e.g. "Beweegminuten"), but falls back to the device locale. Localization strategy is out of scope for this feature — hardcoded Dutch labels are acceptable for MVP.
- Activity title derivation uses simple time-of-day brackets (morning: 05:00–11:59, afternoon: 12:00–16:59, evening: 17:00–20:59, night: 21:00–04:59).
- Exercise route data is read (permissions requested) but not displayed in this feature — reserved for a future map view.
