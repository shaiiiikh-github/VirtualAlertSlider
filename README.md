# Virtual Alert Slider

> **"Your physical alert slider, available digitally."**

**Virtual Alert Slider** is a production-quality Android utility designed to serve as a complete software replacement and backup for physical alert sliders (such as those found on OnePlus or other Android devices). If your phone's physical slider is damaged, broken, stuck, or missing, this application grants full software control over your device's ringer and alert modes independently.

---

## 🌟 Key Features

* 🔔 **Ring Mode**: Instantly set device to Normal ringer mode with call and notification sound enabled.
* 📳 **Vibrate Mode**: Switch to vibration-only mode for calls and notifications.
* 🔇 **Silent Mode**: Full Do Not Disturb (DND) muting for silent environments.
* 🎚️ **Interactive Vertical Slider**: Premium, smooth-dragging vertical slider UI with haptic feedback, snap points, and vibrant Material 3 mode-specific color accents.
* ⚡ **Quick Settings Tile**: Control your alert mode directly from the Android status bar pull-down menu (`TileService`).
* 📱 **Home Screen Widget**: Custom Material 3 widget (`AppWidgetProvider`) with active mode indicators and 1-tap mode switching.
* 🔄 **Real-Time System Synchronization**: Uses dynamic broadcast receivers to detect hardware slider movements, volume key changes, or external system settings adjustments automatically.
* ⚙️ **DataStore Persistence**: Automatically persists selected alert modes, startup defaults, haptic preferences, and theme choices.
* 🎨 **Material 3 Theme**: Clean Obsidian/Slate dark mode default aesthetic, light theme support, and system dynamic color options.

---

## 🛠️ Technology & Stack

* **Language**: Kotlin 2.0.21
* **UI Framework**: Jetpack Compose (Material 3)
* **Target SDK**: Android 15 (API level 35), Min SDK 26 (Android 8.0+)
* **State Management**: Kotlin Flow & StateFlow inside ViewModel
* **Persistence**: Jetpack DataStore Preferences
* **System APIs**: `AudioManager`, `NotificationManager`, `TileService`, `AppWidgetProvider`
* **Build System**: Gradle 8.14.3 (Kotlin DSL with Version Catalog `libs.versions.toml`)

---

## 📱 System Requirements & Android Limitations

### 1. Notification Policy Access (Do Not Disturb Access)
Under standard Android security guidelines (Android 6.0 Marshmallow / API 23+):
* **Ring** (`RINGER_MODE_NORMAL`) and **Vibrate** (`RINGER_MODE_VIBRATE`) can be modified without special permissions on most devices.
* **Silent** (`RINGER_MODE_SILENT` and setting DND Interruption Filter to `INTERRUPTION_FILTER_PRIORITY`) **requires Notification Policy Access**.
* Without this access, attempting to set Silent mode throws an Android `SecurityException`.

#### Graceful Permission Flow
When the user selects **Silent** for the first time without access:
1. The app catches the `SecurityException` gracefully without crashing.
2. A prominent banner & explanation dialog appears.
3. Tapping **Grant Access in Settings** launches the official system intent:
   `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`

### 2. Hardware Alert Slider Coexistence
* Virtual Alert Slider operates **independently** without requiring root privileges.
* When a user toggles the physical slider, Android fires `AudioManager.RINGER_MODE_CHANGED_ACTION`. The app's `RINGER_MODE_CHANGED_ACTION` receiver catches this broadcast and updates the Compose UI, Quick Settings Tile, and Home Screen Widget instantly.

---

## 📂 Project Architecture

The codebase strictly adheres to Android Clean Architecture guidelines:

```text
com.virtualalertslider.app/
├── data/
│   ├── preferences/
│   │   └── PreferencesManager.kt       # DataStore preferences implementation
│   └── repository/
│       ├── AlertSliderRepository.kt     # Repository contract interface
│       └── AlertSliderRepositoryImpl.kt # Repository implementation
├── domain/
│   ├── model/
│   │   ├── AlertMode.kt                 # Ring, Vibrate, Silent enum definition
│   │   ├── SliderSettings.kt            # Settings data model
│   │   └── ThemePreference.kt           # Theme options
│   └── usecase/
│       ├── GetAlertModeUseCase.kt
│       ├── SetAlertModeUseCase.kt
│       ├── ObserveAlertModeUseCase.kt
│       ├── GetSettingsUseCase.kt
│       └── SaveSettingsUseCase.kt
├── system/
│   ├── AudioModeManager.kt              # Interacts with AudioManager & NotificationManager
│   ├── NotificationPolicyManager.kt     # Checks & prompts DND access
│   └── RingerModeReceiver.kt            # Listens to system ringer & DND broadcasts
├── tile/
│   └── VirtualAlertSliderTile.kt        # Quick Settings TileService implementation
├── widget/
│   └── VirtualAlertSliderWidget.kt      # Home Screen AppWidgetProvider implementation
├── ui/
│   ├── components/
│   │   ├── VerticalAlertSlider.kt       # Interactive Compose slider component
│   │   ├── AlertModeCard.kt             # Active mode showcase card
│   │   ├── HeaderCard.kt                # Header & DND status pill
│   │   └── PermissionBanner.kt          # DND permission banner
│   ├── screens/
│   │   ├── main/
│   │   │   ├── MainScreen.kt            # Main screen Compose layout
│   │   │   └── MainViewModel.kt         # ViewModel for main UI
│   │   └── settings/
│   │       ├── SettingsScreen.kt        # App preferences & setup instructions
│   │       └── SettingsViewModel.kt     # ViewModel for settings screen
│   └── theme/
│       ├── Color.kt                     # M3 Slate/Obsidian color tokens
│       ├── Theme.kt                     # Compose MaterialTheme setup
│       └── Type.kt                      # Typography definitions
└── MainActivity.kt                       # Single activity Compose entry point
```

---

## ⚡ Setup & Build Instructions

### Prerequisites
* **JDK**: OpenJDK 17 or higher
* **Android SDK**: API Level 35 (Android 15) installed
* **Gradle Wrapper**: 8.14.3

### Build Commands

```bash
# Clone or open project directory
cd VirtualAlertSlider

# Run Unit Tests
./gradlew test

# Build Debug APK
./gradlew assembleDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Unit Tests

Unit tests are included to verify state transitions, domain use cases, and ringer mode conversions:
* `AlertModeTest.kt`: Validates mode cycling (`RING` -> `VIBRATE` -> `SILENT`), position indexing, and ringer mode conversions.
* `SetAlertModeUseCaseTest.kt`: Validates repository interactions, success state, and `SecurityException` handling when permissions are absent.

To execute tests:
```bash
./gradlew test
```

---

## 📄 License & Attribution

Designed and built as a standalone digital utility for Android devices. Independent implementation with zero proprietary hardware dependencies or brand lock-in.
