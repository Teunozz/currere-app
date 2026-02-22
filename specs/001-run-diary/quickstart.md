# Quickstart: Run Diary

## Prerequisites

- **Android Studio**: Ladybug (2024.2) or newer
- **JDK**: 17+ (bundled with Android Studio)
- **Android SDK**: API 36 installed via SDK Manager
- **Device/Emulator**: Android 16 (API 36) — emulator or physical device
- **Health Connect**: Pre-installed on API 34+ devices. On emulator, ensure Health Connect app is available.

## Build & Run

```bash
# Clone and open in Android Studio
git clone <repo-url>
cd currere
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug
```

Or open the project in Android Studio and run the `app` configuration.

## Testing

### Unit tests

Unit tests cover domain computation logic (pace, splits, aggregations):

```bash
./gradlew test
```

Test files are in `app/src/test/java/nl/teunk/currere/domain/compute/`.

### Manual testing with Health Connect

To test the app, you need running data in Health Connect:

1. **Use a real device with Garmin/Samsung Health/Google Fit**: Record a few runs, then open Currere.
2. **Use the Health Connect test app**: Install the [Health Connect Toolbox](https://developer.android.com/health-and-fitness/health-connect/test) to insert synthetic exercise sessions.
3. **Write test data programmatically**: Create a separate test module or use `adb` to insert Health Connect records via the toolbox.

### Verify key scenarios

| Scenario | Steps |
|----------|-------|
| Run list loads | Open app with HC data present. Verify cards show distance, time, pace. |
| Empty state | Open app with no running sessions in HC. Verify empty state message. |
| Detail screen | Tap a run card. Verify stats, charts, splits. |
| No heart rate | Tap a run without HR data. Verify HR chart is hidden, HR stat shows "—". |
| Pull to refresh | Pull down on run list. Verify data refreshes. |
| Permission denied | Deny permissions on first launch. Verify explanation + settings button. |

## Project Structure

```
app/src/main/java/nl/teunk/currere/
├── data/health/       # Health Connect reads + DTO mapping
├── domain/model/      # Domain models (RunSession, RunDetail, etc.)
├── domain/compute/    # Pure computation (pace, splits, stats)
└── ui/                # Compose screens, ViewModels, theme, navigation
```

## Key Dependencies

| Dependency | Purpose |
|-----------|---------|
| `androidx.health.connect:connect-client` | Health Connect SDK |
| `androidx.compose:compose-bom` | Jetpack Compose alignment |
| `androidx.compose.material3:material3` | Material 3 UI components |
| `androidx.navigation:navigation-compose` | Type-safe Compose navigation |
| Vico | Line charts (heart rate, pace) |
| `kotlinx-serialization-json` | Route argument serialization |
