# RouteSuit — TODO

## Bugs

- **Commute window excludes end hour** — `hour in startHour until endHour` in `WeatherRecommendation.kt:111` uses exclusive `until`, so a 7–9 window never includes 9:00 data. Change to `startHour..endHour`.

- **Coordinate precision loss** — `ConfigRepository.kt` stores `Double` coordinates as `Float` in SharedPreferences (`putFloat`/`getFloat`), losing ~4 decimal places of precision. Switch to `putLong(key, Double.toBits())` / `Double.fromBits(getLong(...))`.

- **Wrong default for precipitation probability threshold** — `ConfigRepository.kt:35` hardcodes `50.0f` as the fallback instead of reading from `defaultConfig.precipitationProbabilityThreshold` (which is 20.0). One-liner fix.

- **Misleading file name** — `LeavePreparedApplication.kt` contains `class RouteSuitApplication`. Rename the file to `RouteSuitApplication.kt`.

## Code Quality

- **Duplicate notification channel setup** — The channel ID `"weather_forecast_channel"` and the code to create the `NotificationChannel` are copy-pasted in both `WeatherNotificationWorker.kt:63` and `SettingsScreen.kt:893`. Extract to a helper in `Constants.kt` + a shared `ensureNotificationChannel(context)` function.

- **Generic notification icon** — `WeatherNotificationWorker.kt:119` uses `android.R.drawable.ic_dialog_info`. Should use a proper app icon resource instead.

- **No unit tests for weather analysis** — `WeatherRecommendation.kt` contains the core business logic but has no tests. Especially the `allTodayCommutesAreDone` guard, the "rain for later" path, and the day-boundary behaviour are tricky and regression-prone.

## UX / Features

- **Battery optimization banner can't be dismissed** — `BatteryOptimizationBanner.kt` re-appears every time the screen is shown. Add a "Dismiss" action that persists to SharedPreferences (same pattern as other settings).

- **12-hour time format support** — `NotificationTimeInput.kt` hardcodes `is24Hour = true` in `TimePickerState`. Should follow the device's time format preference (`android.text.format.DateFormat.is24HourFormat(context)`).

- **Detail sheet buffer is a magic number** — `WeatherDetailSheet.kt:49` hardcodes `windowBuffer = 3`. Expose it as a named constant in `Constants.kt`.

- **Stale-data threshold not configurable** — `Constants.STALE_DATA_THRESHOLD_HOURS` (1 hour) is never surfaced to the user. Either make it a setting or at least document why 1 hour was chosen.

- **Pull-to-refresh shows full loading state** — Refreshing resets to `Loading` UI, replacing the existing data with a spinner. Show the previous data while refreshing instead (SwipeRefresh indicator only).

## Accessibility

- **Emoji not announced to screen readers** — Rain (🌧️) and sun (☀️) emoji used as status indicators in `WeatherRecommendationCard.kt` and notifications have no text alternative. Add `contentDescription` to the emoji `Text` composables, or replace with `Icon` + description.

- **Notification preference change needs explanation** — There's no hint anywhere in the settings UI explaining what the precipitation thresholds actually mean in practice (e.g. "at 20% probability, 4 out of 5 dry days will trigger no notification"). A short subtitle or tooltip would help.
