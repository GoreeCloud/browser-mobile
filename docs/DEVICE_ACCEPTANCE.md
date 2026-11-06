# GoreeCloud Browser Mobile — Android device acceptance notes

**Date:** 2026-11-06 (America/Chicago)  
**Status:** Bounded developer smoke test on one authorized physical device, **not** a Stable/browser security certification.  
**Repository:** https://github.com/GoreeCloud/browser-mobile

## Test environment

- Physical OnePlus N200 (DE2117), Android 16 / API 36, 1080×2400 display at reported 450 density.
- Local Android SDK platform 35, JDK 21, Gradle 9.5, app package `com.goreecloud.browsermobile`, debug build v0.1.0 (min API 26, target API 35).
- The test device contained no preexisting installation of this app. A local debug-signed APK was verified, installed, launched, then upgraded in place after the system-bar fix.
- App-only smoke interactions were performed by ADB. No unrelated app content, contacts, messages, location, or private browsing data was collected.

## Observed outcomes

| Area | Evidence | Result |
| --- | --- | --- |
| Build | `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` | Pass; 20/20 JVM tests; zero lint errors; 13 nonfatal warnings |
| Install & launch | ADB package path, foreground activity and running process | Pass on Android 16 |
| New Tab / Home | Device screenshots | Both distinct surfaces render; core navigation controls visible |
| HTTPS website | An HTTPS page loads in the embedded WebView | Pass for a representative public page; not general compatibility proof |
| URL upgrade/cleaner | VIEW deep link `http://example.com/?utm_source=gc-smoke` | UI reached clean `https://example.com/` without parameter |
| Default filter downloads | App-private `files/filter-lists/` | OISD Small (~1.1 MB) and StevenBlack (~2.2 MB) cached; actual request blocking not independently proven |
| Privacy Center | Device screenshot | Main adblocking, URL cleaner and exact-site controls visible |
| Site allow exception | Enable for `example.com`, inspect app-private prefs, force-stop/restart | Persists across restart; later toggled off and verified `site_exceptions=[]` |
| Status bar contrast | Before/after screenshots | Corrected white-on-light status icons; dark icons in light theme, light icons in dark theme |
| Font scaling | Temporarily set device text scale to 1.3 | New Tab remains legible and usable in screenshot; not full screen-reader coverage |
| Landscape | Temporarily rotated physical device | App renders and bottom navigation is present; some discovery content requires scrolling |
| Device settings cleanup | Readback of settings | Original light mode, 1.0 font scale and portrait rotation restored |

## Constraints and follow-up

- This is **not** full release acceptance. Device test scope covered one physical Android 16 handset, not Android 8–15 or different OEMs.
- Do not imply the subscribed lists block every real ad: the cache and preferences were verified; ad-subresource interception, remote filter update recovery, redirects and false-positive behavior need independent network-level tests.
- System-wide accessibility services/TalkBack, high contrast, keyboard navigation, tablet layouts, performance, stress and full dark-mode flows were not comprehensively audited.
- Backup and device-to-device exclusion settings were previously compiled and linted; they were not experimentally tested against cloud restore/OEM migration.
- Debug APK signatures depend on the build environment. A CI-built APK may not update an APK signed by a different local debug keystore; verify certificate identity before attempting in-place installation.
- No production signing, Play distribution, Stable declaration or deployment occurred. Search remains Startpage-only until GoreeCloud Search passes its separate stability gate.
- Screenshots contain status-bar details and were not added to the public repository. The above observations are a de-identified evidence summary, not an immutable external test lab report.
