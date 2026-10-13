# File Shield Tracker (Android)

An Android security monitoring application that watches file interactions and alerts when unauthorized or suspicious activity takes place.

## Core Features & Flags

* 🟡 **Yellow Flag (Normal Touched/Access):**
  * Detects when files in monitored directories (`Downloads`, `Documents`, `DCIM`, `Pictures`) are accessed, read, modified, or moved while the device is in active, unlocked use.
* 🔴 **Red Flag (Critical / Suspicious Activity):**
  * **File Touched While Device Locked:** Triggers immediately if any external app, service, or background process reads or modifies a file while the device lock screen (`Keyguard`) is active.
  * **Data / File Copied from an App:** Intercepts clipboard events when sensitive data or file URIs are harvested/copied.
  * Pushes high-priority alerts directly to the notification shade.

## Pipeline & APK Builds
This repository includes a continuous integration workflow (`.github/workflows/build-apk.yml`) that triggers on every push to automatically compile and generate the downloadable `app-debug.apk` under GitHub Actions Artifacts.
