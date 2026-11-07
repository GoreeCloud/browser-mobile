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

## November 6 — founder-approved Prism Tabs v5 identity adoption

The founder approved the exact **Prism Tabs v5** source and directed use in both Android Browser Mobile and Linux-first GoreeCloud Browser. The canonical shared source was merged through [brand-assets PR #10](https://github.com/GoreeCloud/brand-assets/pull/10) at commit `1b12ecbb043c89b4f461e68ce2cd83b5fd70eb3f`. It replaces the prior rejected ring-and-arrow Android icon, not the app's navigation or data model.

Verified on the authorized OnePlus N200 Android 16: `adb install -r` succeeded without wiping application data, the modified debug APK launched, and source-grounded visual screenshots showed Prism Tabs v5 in the native browser header and in Android's App Info launcher icon under a circular mask. The packaging retains safe-padded transparent adaptive foreground and themed monochrome derived from the same silhouette. The mono/themed color and full OEM mask range are not independently certified.

Locally `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passed, 26 JVM tests passed, debug APK signature verified with Android v2 and `python3 scripts/check_brand_assets.py` verified 3 central-source SVG blob IDs, 3 PNG SHA-256 fingerprints and adaptive icon links. CI must be checked for the consumer commit separately.

All screenshots remained private on the authorized test computer. No image generation, public APK release, default-browser change or Stable qualification was performed. See [source and derivative mapping](BRANDING.md).

## November 6 — Pinterest-style tab overview real-device acceptance

Source branch `feat/pinterest-tab-overview` replaces the former **Your tabs** ModalBottomSheet with a full-screen, two-column responsive staggered grid. Each open tab has a distinct preview card, title, host-only address, Webspace label, selected border, and 45dp close control. The New action respects the existing 12-tab limit, and Android Back/top arrow returns to browsing. Web thumbnails use a bounded 320×480 RGB_565 snapshot of the already-visible Android WebView and exist only in volatile Kotlin state. Fallbacks are labelled where a real webpage preview is unavailable.

**Verified physical Android 16 OnePlus N200 acceptance:** `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` completed; 26 JVM tests passed; debug APK verified with Android v2 and `adb install -r` succeeded without wiping browsing data. In-app screenshots confirmed: (1) a single native New Tab preview; (2) two staggered cards including an **actual rendered** `https://example.com/` WebView screenshot, *Example Domain* title and host; (3) closing a nonactive tab leaves the active webpage intact and adjusts the visible count from 2 to 1; (4) closing the last website resets it to one blank New Tab without crashing; (5) the New action creates another tab, and tapping a background card returns to its native New Tab page with dock count **2**. No external screenshot service, image generator or analytics upload was involved. Captured test screenshots remained private on the authorized laptop and were not committed.

**Still open:** representative multi-OEM/Android 8–15 screenshots, dark mode, accessibility service/TalkBack, tablet/landscape/large font, memory stress with 12 tabs, protected-content preview behavior and broad browser-security/Stable release acceptance. This is a bounded functional/device check, not a production qualification.

## November 6 — immersive browser chrome, isolated Android 16 device review

**Request:** Match the founder's second screenshot without generating images: remove the redundant logo/product-title header; refine the single pill-shaped HTTPS/search bar and its circular gradient Go control; let the webpage scroll behind the raised bottom dock; auto-hide both browser bars during downward scrolling and reveal on upward scrolling.

**Source:** Native Jetpack Compose changes in `MainActivity.kt` and `core/BrowserChromeScrollPolicy.kt`. A 24px direction-sensitive accumulated scroll threshold responds to `WebView.setOnScrollChangeListener`, without JavaScript injection. Home/New Tab and address editing retain visible controls. The Go icon has an explicit accessibility label inside a 49dp IconButton.

**Verified evidence:**
- GitHub exact-head Android validation succeeded for implementation commit `8759a82cc1c938be507a79e51dd4251acb5cc907` ([CI run](https://github.com/GoreeCloud/browser-mobile/actions/runs/42328908219)).
- On the authorized development laptop, a temporary Ubuntu OpenJDK 21 compiler staging directory (no system package installation) enabled an isolated local Android build. `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` completed **BUILD SUCCESSFUL** against the exact source head; **33 JVM tests passed with 0 failures, errors, or skips**.
- The standalone local **device-review variant** added only `debug.applicationIdSuffix = ".review"` in its temporary, uncommitted working tree, producing package `com.goreecloud.browsermobile.review`. Its APK signature, target SDK and package were verified before installation; `adb install -r` installed the review app separately on a physical OnePlus N200 / Android 16. The preexisting `com.goreecloud.browsermobile` app was not replaced or uninstalled, and no original browsing data was accessed.
- Using only the loaded public GoreeCloud website in the review app, Android UI hierarchy recorded the page's `WebView` bounds `[0,324][1080,2339]` while the visible floating dock's controls lay around y=2205–2270, confirming the webpage is drawn underneath the dock.
- A native downward swipe caused the HTTPS indicator and all six dock controls to disappear from the Android accessibility hierarchy while real webpage text remained visible. A native upward swipe made the HTTPS indicator, Back/Forward/Home/New Tab, tab count and overflow controls reappear.
- No design images or synthetic screenshots were generated or presented. The above is **native build plus bounded accessibility-hierarchy/gesture evidence**, not pixel-level visual signoff or a broad TalkBack/accessibility certification.

**Final exact-source device follow-up:** ADB later reconnected, and the locally recompiled `.review` APK for source head `8759a82` installed successfully in place over **only** the isolated review app. The revised UI hierarchy confirmed the named Go control and HTTPS badge above the webpage; `WebView` bounds became `[0,295][1080,2339]` with the bottom dock still around y=2205–2270. A second physical downward swipe removed the HTTPS, Go, and navigation controls while the webpage continued rendering; an upward swipe restored **Secure HTTPS connection**, **Go to address**, **Back**, **Home**, **Tabs: 1** and **More options**. These observations verify the final compact layout and native hide/reveal interaction on one device. Original Browser Mobile was still not replaced during this isolated test.

**Normal development app installation and cleanup (verified):** After isolated testing, the temporary Gradle `.review` application-ID suffix was reverted in the laptop checkout (no source branch change). The normal `com.goreecloud.browsermobile` debug APK was rebuilt with the exact UI/source revision, all **33 JVM tests** and Lint passed, and its Android app ID and debug signing certificate were confirmed to match the previously installed package. `adb install -r` returned **Success**, updating the existing browser **in place** without an uninstall or a data-wiping operation. `am start` launched the existing package. On the real development app, Android UI hierarchy again confirmed the HTTPS glyph, named Go control, WebView `[0,295][1080,2339]`, bottom dock labels, hide on downward swipe and reveal on upward swipe. Finally, `adb uninstall com.goreecloud.browsermobile.review` returned **Success**; readback showed the original app installed and no `.review` package path. No image generation, released production signing, or broad user-data inspection occurred.

**Remaining limits:** Full pixel-level screenshot comparison to the founder reference, dark mode, landscape, enlarged fonts, keyboard/TalkBack, complex nested website scroll surfaces, and API26–35/multi-OEM behavior remain open. This is a bounded **development preview** device check, not Stable/browser-security certification.

## November 6 — chrome swipe jitter regression, isolated Android 16 verification

After the founder reported intermittent up/down auto-hide glitches, source inspection found a possible reentrancy loop: `setOnScrollChangeListener` interpreted WebView `scrollY` changes caused by the top chrome's expanding/shrinking layout as fresh opposite scroll events. The fix replaces that trigger with a passive, nonconsuming `WebView.setOnTouchListener`, reading **`MotionEvent.rawY`** from a single finger. It requires 56dp directional travel (screen-density adjusted), resets distance on reversal, enforces a 260ms minimum between visibility transitions, and resets gesture state on navigation. It ignores touch cancel/pointer changes and does not inject page JavaScript or capture gesture telemetry.

**Source validation:** Debug APK and Lint built successfully on the authorized Linux laptop with isolated staged JDK 21; **39/39 JVM tests passed**, zero failures/errors. The code-change head `04fef449ccea6794b38a828cb0cbf84ad8630acb` passed [Android GitHub Actions](https://github.com/GoreeCloud/browser-mobile/actions/runs/42330312212). Later documentation-only commits require their own CI result.

**Device regression:** A locally built, temporary application-ID-suffixed `com.goreecloud.browsermobile.review` was installed separately using `adb install -r` on the authorized OnePlus N200 / Android 16. The normal `com.goreecloud.browsermobile` package was not replaced for the isolated test. A public HTTPS GoreeCloud website loaded. Native Android UI hierarchy confirmed the address controls and floating tab dock at rest, with WebView still extending beneath the dock. **Three consecutive downward and upward touchscreen swipe cycles** were issued through ADB and checked against captured on-device UI hierarchies. All three downward gestures hid the address and dock controls while keeping WebView present, and all three upward gestures restored controls while keeping WebView present (3/3 each direction). These checks show stabilized state after each swipe; they are **not** a frame-by-frame animation analysis or universal no-flicker proof.

**Outstanding:** Real-person swipe quality across varied speeds (including short reversals), dark/light, landscape, large fonts, protected sites, nested web containers, touch selection, pinch and assistive technology still need broader review. The test `.review` app should be uninstalled after the production-ID development app has been safely updated (if authorized); do not claim that cleanup or shipping occurred until verified. No release signing, Stable approval, image generation or new permissions were involved.

**Final in-place development upgrade and cleanup (verified):** After the isolated 3-cycle checks, the temporary `.review` Gradle suffix was reverted without committing any signing/package customization. The normal `com.goreecloud.browsermobile` APK built successfully against the same source revision; `39/39` JVM tests and Android Lint passed. We obtained a private rollback APK from the installed app, SHA-256 `509d2412cab05f38467be459a23f0f40e7de11de7ba912c9073ef9e2be5933b6`, then verified the candidate package name and its certificate fingerprint matched the installed app (SHA-256 `1182a19bd68de4fc95cd513a509391026c8e64173f00a38028190fae2f3b68d9`). `adb install -r` returned **Success**, preserving installed application data without uninstall or permission changes. The app launched and a full swipe down/up confirmed hidden/visible chrome with WebView present on both states. A short 40px upward test swipe left the visible bars unchanged, as intended. Finally `adb uninstall com.goreecloud.browsermobile.review` returned **Success**, and package-manager readback confirmed only the original package remained. The private rollback APK was not uploaded or added to the public repo.

This is confirmed Android 16 functional smoke testing only. Human frame-level smoothness, high-refresh-rate behavior, nested scroll and selection gestures, TalkBack/keyboard and cross-OEM variants remain release acceptance work. No image generation or Stable release took place.

## November 6 — automated Android WebView chrome regression suite

**Purpose:** The existing 39 pure JVM tests verify the gesture policy, but do not exercise physical Android touch dispatch, Compose `AnimatedVisibility`, WebView hosting and the floating address/dock together. `app/src/androidTest/java/com/goreecloud/browsermobile/ChromeGestureInstrumentedTest.kt` adds two AndroidX-runner smoke tests: three successive full down/up swipes (both bars hide and return while the WebView remains present), and a 32px partial swipe (bars stay visible). Both tests assert the accessibility hierarchy rather than relying only on screenshots.

**Compilation and provenance:** Android CI now compiles `:app:assembleDebugAndroidTest` in addition to JVM tests, app assembly and Lint. Exact source commit `14e885a0676498a3402c555440e4b06aed479e79` passed [GitHub Actions Android validation](https://github.com/GoreeCloud/browser-mobile/actions/runs/42332498595). Local `:app:assembleDebug :app:assembleDebugAndroidTest` using a temporary application-ID suffix also completed successfully. The local nonessential Lint rerun was stopped during resource contention; GitHub CI completed the full Lint step successfully.

**Real-device execution:** On the authorized physical OnePlus N200 (Android 16), a **local-only** `.qa` debug application ID suffix was used to create and install `com.goreecloud.browsermobile.qa` plus its matching `.qa.test` AndroidX test runner. The test APK's manifest was read back to confirm it targeted only the isolated QA application. Two consecutive `adb shell am instrument -w -r` runs each reported **`OK (2 tests)`** and result code `-1`, for **4/4 test-method executions** including six total up/down gesture cycles. Tests used only the public HTTPS GoreeCloud website and avoided inspecting unrelated on-device user data. Both QA packages were then uninstalled with **Success**; package-manager readback confirmed neither remained, while the original `com.goreecloud.browsermobile` app stayed installed. The temporary local Gradle suffix was reverted, leaving the source checkout clean.

**Scope limitations:** Instrumentation passed on **one Android 16 handset**, not an OEM/API matrix, TalkBack evaluation, animation frame-jank analysis, input-method/accessibility-scrolling audit, or evidence that every nested site scroller has ideal motion. This does not certify Stable release or change production UI behavior. No images were generated, uploaded or committed.

## November 6 — confirmation before destructive browser privacy actions (QA-only device review)

**Source behavior under review:** Four previously immediate actions now require an explicit, cancelable Material3 confirmation: disabling visited-website history (which clears saved website hosts/favicons), clearing recent hosts/favicons, clearing all WebView cookies/web storage, and removing a custom Webspace with its routing rules. Dialog text identifies what is deleted and what is preserved. The old unmerged UI PR #1 contained analogous guards, but the current change ports only these protective workflows without reverting the founder's current immersive browser layout. No stored browser data, permissions, security filtering, or app identity changes were made in source.

**QA instrumentation:** A separate `PrivacyConfirmationInstrumentedTest` seeds **synthetic history host** `confirmation-smoke.example.test` and a **synthetic QA cookie** in a dedicated test-only `.qa` package. It navigates the partially expanded Privacy Center with physical touchscreen swipes, opens each destructive confirmation, chooses **Cancel**, verifies the dialog dismissed, and checks the host/cookie remain intact. The existing `ChromeGestureInstrumentedTest` now explicitly refuses to run unless its target package name ends in `.qa`, preventing accidental execution on a data-bearing normal Browser Mobile installation.

**Evidence:** The source (latest code head `ecb87e78c1a04b45e9d2641a4c7eefc07b58462e`) passed [GitHub Actions Android validation](https://github.com/GoreeCloud/browser-mobile/actions/runs/42334551451), which compiles the app and instrumentation APK, runs the existing **39 JVM tests** and Android Lint. Locally, a staged JDK 21 built the separate QA app and instrumentation APK with `BUILD SUCCESSFUL`. After correcting test-only offscreen-sheet navigation (earlier runs failed because the target rows were outside the initially visible sheet), the physical OnePlus N200 / Android 16 reported **`OK (2 tests)`** for the privacy cancellation class. A subsequent full AndroidX runner execution reported **`OK (4 tests)`** with result code `-1`, combining privacy cancellation checks with the existing two Chrome swipe tests. Both tests used only QA-owned synthetic state or the public GoreeCloud website; **the normal app's stored history/cookies were not read or modified**.

**Cleanup/readback:** The local QA-only `applicationIdSuffix = ".qa"` was never committed. Both `com.goreecloud.browsermobile.qa.test` and `com.goreecloud.browsermobile.qa` were uninstalled successfully and verified absent; the original `com.goreecloud.browsermobile` package remained installed at its existing path. The disposable local checkout's Gradle changes were reverted and Git status read back clean.

**Remaining limits:** Real-device tests directly cover Cancel for local-host history clearing and cookie clearing. Disabling history and custom Webspace removal use the same cancelable confirmation pattern in source but have not yet received separate device-cancel instrumentation; test them before Stable approval. These are bounded developer tests on one Android 16 handset, not a complete accessibility, data-retention, performance, OEM or security certification. No production deployment or image generation occurred.

## November 6 — remaining privacy cancellation paths, physically verified

**Scope:** Follow-up test-only [PR #9](https://github.com/GoreeCloud/browser-mobile/pull/9) extends merged PR #8. `PrivacyConfirmationInstrumentedTest` now checks all four Cancel paths: (1) disabling history with automatic local host/favicons clear, (2) clearing recent hosts/favicons, (3) clearing all WebView cookies/storage, and (4) removing a custom Webspace with its exact-host routing. The new cases seed a synthetic local `confirmation-smoke.example.test` host and a custom `QA Review Space` plus that host's route inside a **separate test-only** application package.

**Safety and assertion detail:** Each test refuses a target package that does not end with `.qa`. On Cancel, Android accessibility queries verify the confirmation closed, and app-private storage is read back to confirm settings, synthetic saved host, cookie, custom Webspace entry and exact-host assignment remain unchanged as applicable. Newly seeded custom Webspace state is cleaned up in test teardown. The history switch is identified by its *screen-row alignment* with its label, not by taking the first switch in the panel. One initial device run failed this selector because Compose exposes multiple switches as separate accessibility nodes; device UI hierarchy inspection showed the correct position, and the test-only selector was corrected. This was a test-targeting failure, not evidence of a production history-disable bug.

**Verification:** Exact code-head `b986b8219ff33e7f29b52d4522945ef5ab8c61ca` passed [Android GitHub Actions run #42336901433](https://github.com/GoreeCloud/browser-mobile/actions/runs/42336901433): existing JVM tests, normal debug app build, instrumented-test APK **compilation** and Android Lint. On the authorized OnePlus N200 / Android 16, the isolated QA debug app and runner built locally with `BUILD SUCCESSFUL`. A targeted privacy test run reported **`OK (4 tests)`**; two subsequent independent full-suite runs each reported **`OK (6 tests)`** with instrumented result code `-1`. The six-test suite includes four privacy-confirmation cancellations plus both real WebView/Compose swipe-stability checks. No screenshots, browser secrets or unrelated private data were captured.

**Cleanup:** The local temporary `debug.applicationIdSuffix = ".qa"` was never committed. Both `com.goreecloud.browsermobile.qa.test` and `com.goreecloud.browsermobile.qa` were uninstalled and confirmed absent. The original browser `com.goreecloud.browsermobile` remained installed at the same package path; its unrelated existing `.test` package was not touched. The disposable checkout's temporary Gradle edit was reverted and Git status read back clean.

**Limits:** These are single-handset Android 16 developer smoke tests for cancellation behavior, not confirmation-action destructiveness tests, cross-OEM/API coverage, TalkBack/keyboard acceptance, performance certification, stable release approval, or production deployment. The source change is test-only; no user browsing data or production runtime code was altered.
