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

No new images were generated for the redesign. **Product-specific official Browser Mobile logo/icon/artwork is still missing from the central brand-assets catalog**; the existing Android launcher vector was reused only. The retained debug app is not a production-ready release.


## November 6 GC UI review branch — pending visual and interaction acceptance

**Scope:** [Draft browser UI PR #1](https://github.com/GoreeCloud/browser-mobile/pull/1) on
`ui/compose-visual-refinement-20261106`. This section is an **acceptance checklist**,
not evidence that the branch has been installed, rendered, or accepted on hardware.
The older device observations above refer to preceding code and do not verify this PR.

The review branch changes the native header/omnibox/dock, Home/New Tab hierarchy,
destination chips and tiles, tab switcher, Privacy Center, and Webspaces. It maps
light/dark palette roles to the canonical GC UI v1 Iris tokens and retains
Startpage as the sole configured search provider. The newer accessibility
follow-up makes full privacy-toggle rows clickable, adapts the header on small
screens, allows horizontal scrolling of destination chips, switches to
single-column website tiles at narrow widths/high font scales, and adds
confirmation before disabling history or removing a custom Webspace. The latest
source refinement also uses explicit GC UI primary/on-primary tokens in the
New Tab dock action, a neutral globe instead of an unconditional address-bar
lock icon, a collapsed optional filter-list editor, screen-reader heading
landmarks, 48dp dock targets, narrow-screen dock insets, and two-level Webspace
cards with separately labeled Move tab / Route site actions and validated
custom-space creation.

### Required device test matrix

| Dimension | Test cases | Pass criteria |
| --- | --- | --- |
| Viewport | narrow ~320dp, typical ~360–412dp, landscape | No inaccessible clipped control, navigation bar overlap, or unexpected horizontal page scrolling |
| Text scale | system font scale 1.0, 1.3, 1.6, 2.0 | Headings wrap, privacy descriptions remain readable, all toggles/actions remain reachable |
| Appearance | light and dark | GC UI semantic foregrounds remain legible; status/navigation bar icon contrast correct |
| New Tab vs Home | launch, +, Home, Back, switch tabs | Clearly distinct surfaces; search remains Startpage; controls work as labeled |
| Search/chrome | typing/IME Go, HTTPS navigation, long URL, loading, failure | Typed text visible, non-truncated function, no fake HTTPS/security indicators |
| Quick destinations | Recent/Favorites/Most visited, empty/1/8 hosts | Horizontal chips reachable; tiles present site identity, counts and URL navigation |
| Tabs | 1, 2, 12; active tab; close last; close inactive | Active state obvious; disabled New Tab at 12; no lost active-tab state |
| Privacy Center | filter on/off, URL cleaner, cookies, site exceptions, DNS shortcut, expand/collapse optional filter lists | Accurate state text; entire setting row activates one switch without double toggles; optional lists are discoverable and remain configurable; unsupported DNS is plainly disclosed |
| Clearing data | disable visited hosts, clear visited hosts, clear cookies | Confirmation and Cancel behavior; only confirmed action erases intended data |
| Webspaces | built-in/custom, move tab, route site, duplicate/invalid names, ten-custom-space maximum, remove custom | Current tab routing is correct; actions remain reachable at 320dp and enlarged text; invalid/duplicate names cannot submit; removal confirmation explains tabs/rules; cookie-isolation warning remains visible |
| Accessibility | TalkBack, headings, switch focus, external keyboard, 48dp navigation targets | Every actionable element has understandable name, role, state and predictable focus order; headings support screen-reader navigation |

### Repeatable Compose UI smoke tests (source and build only)

The review branch adds
`app/src/androidTest/java/com/goreecloud/browsermobile/BrowserChromeInstrumentedTest.kt`
with three focused instrumentation scenarios:

- Home / New Tab separation and navigation via the bottom dock.
- Privacy Center optional-filter list disclosure, expansion, and collapse.
- Webspaces shared-cookie disclosure and nonactionable invalid custom-space creation.

The pull-request CI includes `:app:assembleDebugAndroidTest` to **compile** the
Android UI test APK in addition to JVM tests, debug app assembly, and Lint.
**Compilation is not instrumentation execution.** No UI test is claimed
to have passed on a physical device or emulator until an authorized run
produces test results for the exact build being reviewed.

The app's normal FilterListEngine can refresh enabled HTTPS filter
subscriptions on launch; disclose this network behavior as part of test
consent and avoid incidental access to personal browsing data.

### Safety and evidence requirements

- Confirm target device ownership and obtain any required approval **before** app installation,
  debug package updates, device configuration changes, or access to app-private data.
- Verify debug signature compatibility prior to an upgrade. A CI-built debug APK may
  have a different signing key from the locally installed one.
- Collect screenshots of all six relevant surfaces without exposing browsing data,
  accounts, addresses, notifications, or other personal information.
- Record device model/API/build SHA, exact test conditions, defects, and cleanup.
  Re-run CI and relevant behavioral tests after any corrective source commit.
- This checklist does not certify WCAG, WebView security guarantees, full adblocking,
  Android-version parity, Stable readiness, deployment, or release.

## November 6 review-branch device validation — consented install and regression checks

**Scope and consent.** The owner explicitly authorized device installation during this interaction.
On the authorized OnePlus N200 (DE2117, Android 16/API 36, 1080×2400; physical
density 450 with an existing override of 404), ADB identified an already-installed
`com.goreecloud.browsermobile` debug package. Before installing, the existing APK's
signer and the laptop's existing debug keystore were independently compared:
both reported certificate SHA-256
`1182a19bd68de4fc95cd513a509391026c8e64173f00a38028190fae2f3b68d9`.
No signing key was extracted or uploaded.

**Build and install evidence.** An isolated source checkout pinned to UI source
commit `2a0e4092e0fafc0ade00d5bc0b99612c879d3dfb` passed local
`:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`
using the already-installed JetBrains JDK 21 and Gradle 9.5.0.
[Exact-commit GitHub CI](https://github.com/GoreeCloud/browser-mobile/actions/runs/42297113814)
also completed **SUCCESS**. The local, development-signed app APK had SHA-256
`d67853d90560b01b2064448015cf1a25de462e86132d2098ddd6173d98e08e63`.
`adb install -r` reported **Success**, and package-manager readback confirmed the
installed package path. The app launched and rendered on the device. **No uninstall
or app-data clear was performed.** Existing recent-site cards remained visible on Home;
individual visited hosts and notifications were not recorded in this public report.

**Android 16 instrumentation.** An initial three-test run failed in the old
Espresso runtime before app interactions because it attempted the removed
`InputManager.getInstance()` reflection. Upgrading the **test-only**
dependencies to stable Espresso 3.7.0, AndroidX Test Runner 1.7.0 and ext:junit
1.3.0 corrected the harness; the next three-test run passed **3/3**. The final
test-only commit `f18e20869ac649a36da1b8069f276de1fc4b86fc` compiled an APK whose
app portion is byte-identical to the installed UI commit; the instrumentation
APK passed signing verification and the full on-device suite passed **4/4 in
13.894 seconds**. Scenarios:
- Separate Home and New Tab navigation.
- Privacy Center optional-list expansion and collapse.
- Webspaces shared-cookie warning and disabled invalid custom-space creation.
- Open **Clear recent websites and favicons**, verify confirmation, and choose
  **Cancel** without invoking the destructive action.

**Screenshot-led bounded visual review.** Local app-only screenshots were
captured privately for New Tab, Home, overflow menu, Privacy Center, Webspaces
and the tab switcher; status/navigation-bar regions were cropped before review.
The originally observed half-height settings sheets limited the visible controls.
The UI source was corrected so **Privacy Center and Webspaces expand on opening**,
while the short tab-switcher remains partially expanded. Follow-up screenshots
showed the expanded settings content, six built-in Webspace rows in a scrollable
view, and a correctly labeled active Home tab; no obvious clipping was seen at
the tested **portrait, light-mode** device size. Screenshots containing local site
history were **not** committed or published.

**Important limitations.** This is a bounded app UI/device smoke test,
not full release acceptance or a security/privacy certification. Dark-mode,
increased font scales, landscape, lower Android APIs/OEMs, TalkBack/keyboard,
long-URL/page loading, real request blocking, update recovery, Webspace
site routing, cookie isolation, and destructive **Confirm** paths remain
unverified. The non-destructive Cancel test does not establish that cleared data
can be recovered. The existing host-only visited index and WebView cookies were
not intentionally cleared.

**Mandatory artifact delivery outstanding.** Governing instructions require
each installer APK delivered for testing/release to exist as a standalone
`.apk` in the authorized `GoreeCloud/Artifacts` Drive folder. The tested
APK's checksum, source, and local path are verified, but an authorized binary
transfer into that Drive folder has **not** been completed or confirmed through
available connector capabilities. The GitHub Actions ZIP artifact does **not**
satisfy the standalone Drive requirement and may have a different debug signer.
Do not claim compliant APK delivery, distribute it as a user installer, or
promote the draft PR until that and the remaining acceptance gates are resolved.
