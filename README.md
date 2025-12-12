# RouteSuit

**RouteSuit** is an Android weather app designed to help you prepare for your daily commute. Get personalized weather forecasts for your morning and evening commutes, including temperature and rain alerts.

# Disclaimer
This app is mainly created by AI, with some manual tweaks by me. (As if the overuse of emojis didn't make that clear)

## Features

### 🌤️ Weather Forecasts
- Real-time weather data from SMHI (Swedish Meteorological and Hydrological Institute)
- Forecasts specifically tailored to your commute times
- Temperature and precipitation information
- Support for any location worldwide

### 🌧️ Rain Alerts
- Precipitation probability and amount forecasts
- Smart rain gear recommendations
- Alerts if rain is expected later in the day (even if not during your morning commute)
- Customizable precipitation thresholds

### ⚙️ Highly Customizable
- **Location**: Set coordinates manually or use GPS to get your current location
- **Commute Times**: Configure your morning and evening commute time windows
- **Precipitation Thresholds**: Set when to recommend rain clothes
- **Language**: Support for English and Swedish (with system default option)

### 🔔 Daily Notifications
- Receive daily weather updates for your commutes
- Notifications include temperature and rain gear recommendations

### 🌍 Multi-language Support
- English
- Swedish (Svenska)
- Auto-detect system language

## Requirements

- **Android**: 12.0 (API level 31) or higher
- **Target SDK**: 36
- **Internet Connection**: Required for weather data
- **Location Permission**: Optional (for automatic location detection)

## Installation

### From Source

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd RouteSuit
   ```

2. Open the project in Android Studio (Hedgehog or later recommended)

3. Sync Gradle dependencies

4. Build and run the app on your device or emulator

### Building the APK

```bash
./gradlew assembleDebug
```

The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

## Usage

### First Launch

1. **Set Your Location**: 
   - Tap the Settings icon (⚙️) in the top right
   - Enter coordinates manually, or
   - Tap "Get Current Location" to use GPS

2. **Configure Commute Times**:
   - Set your morning commute start and end hours (24-hour format)
   - Set your evening commute start and end hours

3. **Customize Settings** (Optional):
   - Modify precipitation thresholds
   - Configure commute times
   - Configure notification days and times

### Daily Use

- Open the app to see weather forecasts for your next commute
- View temperature and rain alerts
- Pull down to refresh the forecast (pull-to-refresh)
- Check notifications for daily weather updates

## Configuration

### Location Settings
- **Longitude**: -180 to 180
- **Latitude**: -90 to 90
- Use "Get Current Location" button for automatic detection

### Commute Times
- Configure in 24-hour format (0-23)
- Default: Morning 7-9 AM, Evening 4-7 PM

### Precipitation Thresholds
- **Probability Threshold**: Minimum percentage to recommend rain clothes (default: 20%)
- **Amount Threshold**: Minimum expected precipitation in mm (default: 0.5 mm)

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with dependency injection
- **Dependency Injection**: Koin
- **Networking**: Retrofit 3.0 with OkHttp and Gson
- **Logging**: Timber
- **Coroutines**: Kotlin Coroutines for async operations
- **Navigation**: Navigation Compose
- **Background Work**: WorkManager for notifications
- **Location Services**: Google Play Services Location
- **Material Design**: Material 3

## Permissions

The app requires the following permissions:

- **INTERNET**: To fetch weather data from SMHI API
- **POST_NOTIFICATIONS**: To send daily weather notifications (Android 13+)
- **ACCESS_FINE_LOCATION**: Optional, for automatic location detection
- **ACCESS_COARSE_LOCATION**: Optional, for automatic location detection

## API Information

This app uses the **SMHI Open Data API** for weather forecasts:
- **API Endpoint**: `https://opendata-download-metfcst.smhi.se/api/`
- **Data Format**: JSON
- **Forecast Type**: Snow/Precipitation forecast (snow1g)
- **Update Frequency**: Real-time as requested

The API is free and open, but please respect their usage terms.

## Project Structure

```
app/src/main/java/se/isakalmgren/routesuit/
├── MainActivity.kt              # Main activity and navigation
├── WeatherScreen.kt             # Weather forecast UI
├── SettingsScreen.kt            # Settings and configuration UI
├── WeatherRecommendation.kt     # Weather analysis logic
├── SmhiApiService.kt            # API service interface
├── AppConfig.kt                 # Configuration data class
├── ConfigRepository.kt          # Configuration persistence
├── LocationHelper.kt            # Location services
├── NotificationScheduler.kt     # Notification management
├── WeatherNotificationWorker.kt # Background notification worker
├── LanguageRepository.kt        # Language/locale management
├── AppModule.kt                 # Dependency injection setup
├── Constants.kt                 # Application constants
└── ui/theme/                    # Material Design theme
```

## Development

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11 or higher
- Android SDK 36

### Building
```bash
./gradlew build
```

### Running Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

[Add your license here]

## Author

Developed by AI, with some manual tweaks by me.

---

**Note**: This app is designed for personal use and provides weather recommendations based on forecast data. Always use your best judgment when preparing for weather conditions.

