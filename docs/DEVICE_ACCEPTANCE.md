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

## November 6 UI/Webspaces redesign smoke test (additional)

The same authorized OnePlus N200 / Android 16 handset received an **in-place upgrade** of the locally debug-signed APK. Installation succeeded and the app launched. Bounded screenshots and app-private preference inspection confirmed:

- The existing G-vector launcher artwork now appears in the header instead of text-only "G"; the top privacy badge is gone.
- The Home banner is removed. The Quick Destinations selector shows **Recent / Favorites / Most visited**; initial empty state did not show synthetic letter placeholders.
- A successful visit to `https://example.com/` created a single app-private history entry containing only `example.com`, count and timestamp. Home then showed it in Recent with a neutral globe fallback.
- A visit to `https://www.wikipedia.org/` created a second host-only entry and saved one ~4.4 KiB favicon in app-private `files/site-favicons/` from WebView. No third-party favicon collection endpoint is used.
- The tab icon visibly shows **1**, and the three-dot toolbar menu renders as a small, anchored pop-up instead of the former full-width draggable sheet. The menu opens the Webspaces organizer.
- The Webspaces organizer renders built-in spaces and visibly warns that WebView cookies and signed-in accounts are **not isolated**. It shows tab counts and supports local custom-space configuration, exact-host routing and manual tab moves in source.
- One-device screenshots are visual smoke evidence, not a comprehensive accessibility, privacy, site routing or Android-version matrix.

No image-generation tool was used for that redesign. At the time of this earlier test, the product-specific official Browser Mobile identity was absent from the central brand-assets catalog, and the previous Android launcher vector was used. A separate **later November 6 UI pass** authored, centrally published, and installed a replacement portal icon; see the next section. Neither test establishes production-release readiness.

## November 6 founder New Tab mockup acceptance

See [source-grounded New Tab mockup audit](NEW_TAB_MOCKUP_AUDIT.md) and [UI acceptance notes](UI_ACCEPTANCE.md). The same OnePlus N200 was used for an in-place debug APK update to test the native wave, Startpage card, four Quick Access controls, dark and light mode, landscape, 130%/100% text size, and preserved neutral floating navigation. The OS Downloads activity launched from the Downloads shortcut, and the Startpage web destination loaded. The local v2-signed debug APK passed 26 JVM tests, Lint and assembly. Device settings were restored after testing. No screenshots were committed and no image-generation service was used.

## November 6 — premium New Tab and identity rework after founder rejection

Source improvements now include a simplified open-ring/forward-arrow vector (header, adaptive launcher and monochrome), canonical central brand-assets revision [e02c3d5](https://github.com/GoreeCloud/brand-assets/commit/e02c3d5), a raised search field, grouped editorial New Tab hero, subtle native orbit ambience, an elevated navy Startpage feature card and tonal Quick Access tiles. Up to two real visited hosts appear beneath the shortcuts when app-private website-host history is enabled. The founder-requested neutral New Tab action and removal of the top privacy badge were preserved.

A debug build was installed on the authorized OnePlus N200 and visually inspected in light/dark and at 100%/130% text. In the initial screen review, the dot/eyebrow floated too far from the heading; grouping those elements into one hero was the final polish correction. No AI image generation occurred. This is source-and-device preview validation, **not founder visual approval or comprehensive accessibility acceptance**.
