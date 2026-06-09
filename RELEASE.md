# Release Process

## Versioning

The app uses two version identifiers that must both be updated for every release.

### versionCode
An integer that must strictly increase with every upload to the Play Store. The store uses this to determine which build is newer — it is never shown to users.

Convention used here: `MAJOR * 10000 + MINOR * 100 + PATCH`

| versionName | versionCode |
|-------------|-------------|
| 1.0.0       | 10000       |
| 1.1.0       | 10100       |
| 1.1.1       | 10101       |
| 2.0.0       | 20000       |

### versionName
A human-readable string shown to users on the Play Store. Follow [Semantic Versioning](https://semver.org/):
- **MAJOR** — breaking changes or significant redesign
- **MINOR** — new features, backwards compatible
- **PATCH** — bug fixes only

Both are set in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 10000   // bump this every release
    versionName = "1.0.0" // human-readable
}
```

---

## One-time Setup

### 1. Create a release keystore

The keystore signs every release build. **Keep this file and its passwords safe — you cannot re-sign existing Play Store releases with a different key.**

```bash
keytool -genkeypair -v \
  -keystore routesuit-release.jks \
  -alias routesuit \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Store `routesuit-release.jks` somewhere outside the repo (e.g. `~/.android/routesuit-release.jks`). Add it to `.gitignore` if it ever ends up in the project directory.

### 2. Store signing credentials

Create `keystore.properties` in the project root (already in `.gitignore`):

```properties
storeFile=/home/isak/.android/routesuit-release.jks
storePassword=your_store_password
keyAlias=routesuit
keyPassword=your_key_password
```

### 3. Wire signing into the build

Update `app/build.gradle.kts`:

```kotlin
import java.util.Properties

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps["storePassword"] as String
            keyAlias = keystoreProps["keyAlias"] as String
            keyPassword = keystoreProps["keyPassword"] as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 4. Create a Play Store developer account

- Go to [play.google.com/console](https://play.google.com/console) and pay the one-time $25 registration fee.
- Create a new app, set the app name to **RouteSuit**, and choose "Apps" / "Utility".

---

## Release Checklist

### Before building

- [ ] Bump `versionCode` and `versionName` in `app/build.gradle.kts`
- [ ] Commit the version bump: `git commit -m "chore: bump version to 1.x.x"`
- [ ] Tag the commit: `git tag v1.x.x && git push --tags`
- [ ] Verify `keystore.properties` exists and is correct

### Build the release AAB

The Play Store requires an **Android App Bundle** (`.aab`), not an APK.

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

To also verify the APK locally:

```bash
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

### Play Store upload

1. Go to Play Console → RouteSuit → **Release** → **Production**
2. Click **Create new release**
3. Upload `app-release.aab`
4. Fill in the **Release notes** (what changed for the user)
5. Review, then **Roll out**

---

## Play Store assets needed (first release)

| Asset | Spec |
|-------|------|
| App icon | 512×512 PNG, no alpha |
| Feature graphic | 1024×500 PNG or JPG |
| Screenshots (phone) | 2–8, min 320px on short side |
| Short description | max 80 chars |
| Full description | max 4000 chars |

The `icon2.png` (500×500) in the repo is close — scale it up to 512×512 before uploading.

---

## Ongoing release flow

```
bump versionCode + versionName
git commit + git tag vX.Y.Z
./gradlew bundleRelease
upload AAB to Play Console
write release notes
roll out to production
```

---

## Notes

- **Play App Signing**: Google manages the final signing key used on devices. The key you create above is the "upload key". If you ever lose the upload key you can request a reset through the Play Console.
- **Internal testing track**: Use the Internal Testing track for every build before promoting to Production. It reaches testers within minutes and doesn't affect the public rollout.
- **minSdk 31** means the app targets Android 12+, covering ~85% of active devices as of 2025.
