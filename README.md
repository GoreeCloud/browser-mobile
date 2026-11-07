# GoreeCloud Browser Mobile

**Native Android browser · early development preview · MIT-licensed**

GoreeCloud Browser Mobile is an original, privacy-oriented Android browser built in Kotlin with Jetpack Compose and the Android system WebView. The browser chrome is native; websites are rendered using Android WebView. It is **not** a VPN, a private DNS resolver, or a mature replacement for a security-reviewed browser.

## Verification scope

The Android workflow runs JVM policy tests, compiles the instrumentation APK, assembles the debug app and runs Lint. Actual touch/Compose/WebView chrome checks are in `app/src/androidTest/` and **must run only on an authorized test device under an isolated QA application ID**, never against a data-bearing browser installation. The device regression evidence and cleanup procedure are recorded in [Device acceptance](docs/DEVICE_ACCEPTANCE.md). Passing either suite is development-validation evidence, not a Stable release gate.

## Preview features

- **Two deliberately different starting surfaces:** the founder-mockup-inspired **New Tab** now uses a cohesive editorial hero with a subtle native orbit ambient, elevated navy Startpage card, semantic tonal shortcut icons, and up to two *actual locally visited hosts* (only when history is enabled) for quick return. Bookmarks, host-only History, Android Downloads and Privacy Settings are functional. Shortcuts adapt from 4 columns to 2×2 for small screens or enlarged text. The separate Home retains dynamic destinations and privacy statistics. [New Tab audit](docs/NEW_TAB_MOCKUP_AUDIT.md) records the corrections and nonrelease boundaries.
- **Startpage search by default**, with a fixed, documented policy that prevents premature migration to GoreeCloud Search.
- **Immersive floating Android browser chrome:** the permanent logo/name header is removed in favor of a premium address/search pill and circular Go action. The bottom navigation floats over webpages. Both bars hide after a deliberate downward webpage swipe and return with an upward swipe or explicit navigation. Direction is measured from nonconsuming native one-finger motion (56dp travel, reversal filtering and a short cooldown), **not** WebView scroll-position callbacks that can react to UI layout changes. Home/New Tab and address editing keep controls visible. No website script injection or scroll telemetry. [Architecture and acceptance](docs/ARCHITECTURE.md).
- **Native tabs** (up to 12 per session) with a visible tab count, back/forward, address/search, reload, sharing, and bookmarks. Tapping the floating dock's tab-count button opens a **full-screen, Pinterest-style staggered overview** of the open tabs with real in-memory viewport thumbnails when available, clear close controls, tap-to-switch and a New action. Built-in Home/New Tab have recognizable native preview cards; unavailable WebView screenshots are explicitly labeled. The bottom dock retains neutral buttons, and the three-dot control opens a compact grouped action palette instead of a sliding sheet. See [tab overview and privacy scope](docs/TAB_OVERVIEW.md).
- **Webspaces organizational preview:** Standard, GoreeCloud, Google, Microsoft, Meta, Proton, plus up to 10 local custom spaces. Group/open/move tabs, apply exact-host routing rules and use safe provider-domain defaults. **This is not Firefox contextual-identity isolation**: Webspaces share Android WebView cookies, authentication, and website storage. See [Webspaces scope](docs/WEBSPACES.md).
- **Home Quick Destinations:** eight recently visited hosts, eight bookmarked favorites, or eight most-visited hosts, selected with local chips. Visited-site favicons come only from WebView; a globe replaces missing icons. No fixed letter tiles or oversized Home hero. The optional local history index stores host, visit count and timestamp (not paths, titles, search queries or fragments), caps at 100 hosts, and can be disabled or cleared in Privacy Center.
- **URL cleaning** for a conservative set of known advertising/campaign parameter names. Necessary non-tracking query parameters are preserved.
- **Built-in ad/tracker domain subscriptions**: OISD Small and StevenBlack Unified are **enabled by default**. The user can opt into OISD Big (replaces Small), OISD NSFW Small, or StevenBlack Gambling; disabled lists do not download. Lists are fetched only over HTTPS, updated at app startup when older than 24 hours, locally cached with last-known-good fallback, and manually refreshable. A reversible **Allow ads on this site** switch pauses request filtering for that exact top-level site; other sites remain protected. This provides domain-based WebView subresource filtering only—not cosmetic filtering or exhaustive ad blocking. See [filter subscription policy](docs/FILTER_SUBSCRIPTIONS.md).
- **Find in Page (native):** open from More options while browsing a website to search the *current rendered WebView*, see match counts, jump to next/previous occurrence and close the search with highlights cleared. Queries are bounded, held in volatile tab state and never added to history or sent to Startpage.
- **Per-tab Text Size:** open from More options to adjust webpage text zoom in 20% steps from **80% to 200%**, or reset to 100%; each tab retains its own in-memory setting, which is not stored by hostname or synchronized. Some sites may override text scaling. [Page tools scope](docs/PAGE_TOOLS.md).
- **Cookie controls** for global cookie acceptance, third-party cookies (off by default), and explicit clearing of cookie and web-storage data.
- **System DNS manager entry point** to Android Private DNS settings. Android WebView uses the OS DNS stack; per-app custom DNS/DoH is **not** implemented.
- **Founder-approved Prism Tabs v5 branding:** the rejected ring-and-arrow logo is replaced with a directly rendered, source-pinned icon from [GoreeCloud's canonical browser-family brand master](https://github.com/GoreeCloud/brand-assets/blob/1b12ecbb043c89b4f461e68ce2cd83b5fd70eb3f/products/browser-mobile/app-icon.svg). It appears in the Android header and launcher, with separately padded adaptive/color and monochrome foregrounds. The **same app icon** is shared with Linux-first [`GoreeCloud/browser`](https://github.com/GoreeCloud/browser); the mobile and desktop wordmarks use different subtitles. [Branding provenance and platform limits](docs/BRANDING.md). Icon design approval does not imply Stable release or complete themed-launcher accessibility testing.
- **HTTPS-first browsing:** bare and explicit `http://` addresses are upgraded to HTTPS; top-level HTTP GET links are upgraded where WebView exposes navigation callbacks, without a cleartext or TLS-failure downgrade fallback. Rewritten non-GET requests are canceled rather than replayed with changed methods. Legacy HTTP-only websites may not work.
- Defensive WebView defaults: no file/content access, mixed-content prohibition, Safe Browsing enabled, no auto popups, explicit denial of browser device-permission prompts, and SSL-error cancellation.
- **Device-local data protection**: Android cloud backup is disabled, and explicit Android 12+ data-extraction rules exclude browser data from both cloud backup and device-to-device transfer. Android themed-launcher icons are supported.

## Search policy

**Only Startpage is used for search until GoreeCloud Search is verified Stable.**
Do not change the provider merely because a repo, web client or experimental endpoint exists. See [docs/SEARCH_ENGINE_POLICY.md](docs/SEARCH_ENGINE_POLICY.md) for adoption criteria. There is no undocumented search-engine picker and no background switch-over.

## Build

Requires an Android SDK with platform 35, JDK 17+, Gradle 9.5.0, and network access to AndroidX and Android Gradle Plugin repositories.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The app targets Android 8.0+ (API 26) and SDK 35. A passing build is **not** sufficient evidence of browser-security, UI, device, or release acceptance. No production release or Android Play Store distribution has been verified.

## Privacy notes and limitations

- Websites are loaded over the network; browsing is not anonymized. Startpage's external privacy policy applies to searches.
- JavaScript and DOM storage are enabled for site compatibility.
- Bookmarks, Webspace configuration, and optional website-level history are stored locally in app-private Android storage. The history stores only a website hostname, visit count and timestamp, not individual page URLs or search queries; up to 100 hosts are retained, with explicit pause/clear controls. Cached website favicons are also app-private and can be cleared. Tabs and visited URLs are not synced. Browser websites can still use regular WebView storage unless cleared.
- Domain-list blocking is not equivalent to uBlock Origin: first-party/same-host ads, cosmetic placeholders, video ads and some requests are not blockable with WebView's subresource interception. The app downloads selected public lists directly from their publishers; subscriptions are unbundled and use the publishers' respective distribution terms.
- DNS settings are delegated to Android. A dedicated DNS-over-HTTPS resolver would require a separately evaluated networking architecture; exposing a DNS setting does not implement it.
- Complete in-browser download management (the New Tab Downloads shortcut currently delegates to Android Downloads), desktop-site emulation, session restoration, granular site permissions, built-in password management, and per-site cookie deletion remain future work. Find in Page and Text Size are current-tab local tools, not replacements for those features.
- There has not yet been a full device/browser compatibility, accessibility, privacy leak, or WebView security audit.

## Architecture and development

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for component boundaries, privacy trade-offs, and risk-based acceptance. [Android device acceptance notes](docs/DEVICE_ACCEPTANCE.md) record bounded physical Android 16 smoke tests, corrected light/dark system-bar contrast, and remaining release gaps. CI compiles the native APK, runs JVM unit tests, executes Android Lint, and temporarily retains its debug APK as a 7-day internal Actions artifact. **A CI debug APK may be signed with a different certificate from the locally installed build.** The full APK delivery requirement is governed by GoreeCloud's canonical Google Drive instructions: installable APKs delivered for testing must be placed individually in `GoreeCloud/Artifacts/` after build provenance and identity checks, not delivered as GitHub Actions ZIPs.

## Open source

The GoreeCloud-authored source in this repository is licensed under the [MIT License](LICENSE). Public filter-list data is retrieved **separately at runtime** and remains under its owners' licenses: OISD (GPL-3.0) and StevenBlack's aggregated, multi-source hosts data (including upstream attribution/licenses). See [docs/FILTER_SUBSCRIPTIONS.md](docs/FILTER_SUBSCRIPTIONS.md). Android, Gradle, Kotlin and AndroidX are third-party components under their own licenses; GoreeCloud does not claim ownership of those dependencies or of the external list data.
