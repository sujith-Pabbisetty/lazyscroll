# LazyScroll 📜👁️

[![Download APK](https://img.shields.io/badge/Download-LazyScroll%20APK-2ea44f?style=for-the-badge&logo=android&logoColor=white)](https://github.com/sujith-Pabbisetty/lazyscroll/raw/main/lazyscroll.apk)

**LazyScroll** is an innovative Android application designed for hands-free scrolling using CameraX eye-blink detection via Google ML Kit and Android Accessibility Services.

---

## 📥 Download & Install

Click the button below to download the latest APK directly:

[<img src="https://img.shields.io/badge/Download_APK-v1.0-blue?style=for-the-badge&logo=android" height="45">](https://github.com/sujith-Pabbisetty/lazyscroll/raw/main/lazyscroll.apk)

Or download directly using this link: **[lazyscroll.apk](https://github.com/sujith-Pabbisetty/lazyscroll/raw/main/lazyscroll.apk)**

---


## 🌟 Key Features

1. **Eye-Blink Detection (CameraX + ML Kit)**: Uses the front camera to detect user blinks and trigger automated scrolling actions without touching the screen.
2. **Accessibility Service Integration**: Utilizes Android's `AccessibilityService` API to perform smooth continuous scrolling over any application.
3. **Floating Overlay Controls**: Provides a `SYSTEM_ALERT_WINDOW` floating widget allowing quick play/pause toggling over running apps.
4. **Modern Android Architecture**: Built using Kotlin, AndroidX, Material Design 3, Gradle Version Catalogs (`libs.versions.toml`), and target SDK 35.

---

## 📁 Repository Structure

```
lazyscroll/
├── app/
│   ├── build.gradle.kts       # Module build script & dependencies
│   ├── proguard-rules.pro     # Release obfuscation rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/lazyscroll/
│       │   │   ├── MainActivity.kt            # Permission setup & control panel
│       │   │   └── MyAccessibilityService.kt  # Accessibility & blink detection logic
│       │   └── res/                          # Layouts, XML configs, values, drawables
│       ├── test/                             # Unit tests
│       └── androidTest/                      # Instrumented UI tests
├── gradle/
│   ├── libs.versions.toml                    # Version catalog (dependencies management)
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts                           # Top-level build config
├── settings.gradle.kts                        # Repository management & module inclusion
├── gradle.properties                          # JVM & AndroidX settings
├── gradlew & gradlew.bat                      # Gradle wrapper executable scripts
└── .gitignore                                 # Clean git ignore configuration
```

---

## 🚀 Getting Started

### Requirements
- **Android Studio**: Ladybug / Jellyfish or newer
- **JDK**: Java 11 or higher
- **Android Device / Emulator**: Android 7.0+ (API 24+) with front camera support

### Building & Running
1. Open Android Studio and select **Open**.
2. Select the `lazyscroll-github` directory.
3. Allow Gradle Sync to finish automatically.
4. Connect an Android device or launch an emulator.
5. Click **Run** (`Shift + F10`).

### Granting Required Permissions
Upon first launch, LazyScroll requires three permissions to operate:
1. **Overlay Permission (`SYSTEM_ALERT_WINDOW`)**: Allows drawing the floating control button over other apps.
2. **Camera Permission**: Allows the front camera to track eye blinks via ML Kit.
3. **Accessibility Service**: Enable `LazyScroll Service` in Android Settings -> Accessibility.

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
