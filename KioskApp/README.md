# KioskApp — Android Kiosk Browser

A production-ready Android kiosk application that:
- **Auto-launches on boot** (powers on → app starts automatically)
- **Full-screen WebView** (status bar and navigation bar fully hidden)
- **Loads a specific HTTPS URL** (configurable in one place)
- **On-screen keyboard** appears automatically when the user taps any text field
- **Locks navigation** to your domain (optional)
- **Auto-retries** on connection errors

---

## Quick Setup (3 steps)

### 1. Set your URL
Open `app/src/main/java/com/kiosk/app/KioskActivity.java` and change line 35:

```java
private static final String KIOSK_URL = "https://your-url-here.com";
```

Replace `https://your-url-here.com` with your actual HTTPS address.

### 2. Build the APK
Open the project in **Android Studio** (Electric Eel or newer):

```
File → Open → select the KioskApp folder
```

Then build:
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

The APK will be output to:
```
app/build/outputs/apk/debug/app-debug.apk
```

For a release APK (smaller, optimised):
```
Build → Generate Signed Bundle / APK → APK → follow signing wizard
```

### 3. Install on device

**Via ADB (USB):**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Via file transfer:**
Copy the APK to the device, open it in a file manager, tap to install.
(Device must have "Install from unknown sources" enabled in Settings.)

---

## Configuration Options

All options are at the top of `KioskActivity.java`:

| Constant | Default | Description |
|---|---|---|
| `KIOSK_URL` | `https://your-url-here.com` | The URL to load |
| `LOCK_TO_URL` | `true` | Block navigation outside your domain |
| `RELOAD_DELAY` | `5000` | ms to wait before retrying on error |
| `SHOW_PROGRESS` | `true` | Show loading progress bar |

---

## Screen Orientation

Default is **landscape**. To change to portrait, edit `AndroidManifest.xml`:

```xml
android:screenOrientation="portrait"
```

---

## How It Works

| Feature | Implementation |
|---|---|
| Auto-start on boot | `BootReceiver` catches `BOOT_COMPLETED` broadcast |
| Full-screen | Immersive sticky mode hides nav/status bars |
| On-screen keyboard | Native Android — appears automatically on text field focus |
| Domain lock | `WebViewClient.shouldOverrideUrlLoading` blocks external URLs |
| Error recovery | Auto-retries every 5 seconds; manual Retry button shown |
| Screen-always-on | `FLAG_KEEP_SCREEN_ON` window flag |

---

## Device Requirements

- Android 5.0 (API 21) or higher
- Internet connection
- "Install from unknown sources" enabled (for sideloading)

---

## Optional: True Kiosk Lockdown

For stronger lockdown (prevent users from ever exiting), the device can be
set as a **Device Owner** using Android's Device Policy Manager. This requires
enrolling the device via ADB:

```bash
adb shell dpm set-device-owner com.kiosk.app/.AdminReceiver
```

This is an advanced step — contact your MDM/EMM provider or Android developer
for assistance if required.
