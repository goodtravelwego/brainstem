# Test without opening Android Studio

You can run the APK on an Android phone or emulator. Native Kotlin Android screens do not run as a Windows desktop app or directly in a web browser.

## Install the ready APK on a phone

Copy `BrainSTEM-Stimulate-debug.apk` to a phone running Android 8.0 or newer. Open it and allow installation from the file/browser app when Android asks. Launch **BrainSTEM Stimulate**. The guest option on the login page runs the offline simulator. This debug build is for testing; Play Store distribution needs a signed release build.

## Build and install from PowerShell

Android Studio does not have to be open. This computer already has the JDK and SDK it installed:

```powershell
Set-Location C:\dev\brainstem
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
$brainstemAdb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $brainstemAdb devices
```

Enable USB debugging on the phone and accept its computer authorization prompt. Replace `YOUR_DEVICE_SERIAL` below with the device shown by `adb devices`:

```powershell
& $brainstemAdb -s YOUR_DEVICE_SERIAL install -r app\build\outputs\apk\debug\app-debug.apk
& $brainstemAdb -s YOUR_DEVICE_SERIAL shell am start -n com.brainstem.stimulate/.MainActivity
```

The `-r` option updates an app signed with the same debug key while preserving its local data. Do not uninstall an existing installation just to resolve a signing-key error unless you have decided to discard its local records.

## Start an existing emulator from the command line

```powershell
$brainstemEmulator = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
& $brainstemEmulator -list-avds
& $brainstemEmulator -avd YOUR_AVD_NAME
```

Use the emulator's serial from `adb devices` in the install/start commands. A computer without Android Studio can instead install Java 25 and Google's Android command-line tools, platform-tools, SDK platform 37 (`platforms;android-37.0`), and build-tools 36.0.0. Set its SDK path in an untracked `local.properties`, then use the same Gradle wrapper.

Reference: [Android's command-line build and installation guide](https://developer.android.com/build/building-cmdline).
