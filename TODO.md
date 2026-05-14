# RouteSuit — TODO

## Bugs

- ~~**Commute window excludes end hour**~~ — Fixed: `until` → `..` in `WeatherRecommendation.kt`.

- ~~**Coordinate precision loss**~~ — Fixed: coordinates now stored as `Long` bits in SharedPreferences with Float fallback for migration.

- ~~**Wrong default for precipitation probability threshold**~~ — Fixed: now reads from `defaultConfig` instead of hardcoded `50.0f`.

- ~~**Misleading file name**~~ — Fixed: renamed to `RouteSuitApplication.kt`.

## Code Quality

- ~~**Duplicate notification channel setup**~~ — Fixed: extracted to `NotificationHelper.kt` (`ensureNotificationChannel` + `buildWeatherNotification`). Both Worker and SettingsScreen now delegate to it.

- **Generic notification icon** — `WeatherNotificationWorker.kt:119` uses `android.R.drawable.ic_dialog_info`. Should use a proper app icon resource instead.

- **No unit tests for weather analysis** — `WeatherRecommendation.kt` contains the core business logic but has no tests. Especially the `allTodayCommutesAreDone` guard, the "rain for later" path, and the day-boundary behaviour are tricky and regression-prone.

## UX / Features

- **Battery optimization banner can't be dismissed** — `BatteryOptimizationBanner.kt` re-appears every time the screen is shown. Add a "Dismiss" action that persists to SharedPreferences (same pattern as other settings).

- ~~**12-hour time format support**~~ — Fixed: `NotificationTimeInput.kt` now reads `DateFormat.is24HourFormat(context)` and adjusts both the picker and display text accordingly.

- ~~**Detail sheet buffer is a magic number**~~ — Fixed: now `Constants.DETAIL_SHEET_BUFFER_HOURS`.

- **Stale-data threshold not configurable** — `Constants.STALE_DATA_THRESHOLD_HOURS` (1 hour) is never surfaced to the user. Either make it a setting or at least document why 1 hour was chosen.

- **Pull-to-refresh shows full loading state** — Refreshing resets to `Loading` UI, replacing the existing data with a spinner. Show the previous data while refreshing instead (SwipeRefresh indicator only).

## Accessibility

- **Emoji not announced to screen readers** — Rain (🌧️) and sun (☀️) emoji used as status indicators in `WeatherRecommendationCard.kt` and notifications have no text alternative. Add `contentDescription` to the emoji `Text` composables, or replace with `Icon` + description.

- **Notification preference change needs explanation** — There's no hint anywhere in the settings UI explaining what the precipitation thresholds actually mean in practice (e.g. "at 20% probability, 4 out of 5 dry days will trigger no notification"). A short subtitle or tooltip would help.
