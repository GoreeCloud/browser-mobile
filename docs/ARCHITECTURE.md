# Architecture & risk boundaries

## Components

- `MainActivity.kt`: native Compose toolbar, Home, New Tab, tabs, privacy settings, Android WebView host. The founder-mockup-inspired New Tab was reworked into a grouped editorial hero with low-opacity native orbit ambience, elevated dark Startpage card, four functional semantic shortcut tiles, and up to two previously visited hosts when local history is enabled. Bookmarks / website-host History / Android Downloads / Privacy Center actions remain real. The four shortcuts collapse to a two-column grid at narrower widths or enlarged text. [Audit and limitations](NEW_TAB_MOCKUP_AUDIT.md).
- `core/BrowserChromeScrollPolicy.kt`: pure JVM-tested, gesture-direction policy. The old `setOnScrollChangeListener` approach interpreted WebView layout-induced `scrollY` changes as gestures when `AnimatedVisibility` shrank/expanded the top bar, causing feedback flicker. The replacement listens passively to Android WebView touch events, always returning `false` so WebView retains taps, scrolling and selection. A one-finger swipe of 56dp of consistent screen-space `MotionEvent.rawY` travel changes visibility, with reversal reset, 260ms cooldown, and reset on navigation/tab changes. Raw Y is independent of WebView viewport resizing; no injected website JavaScript, event telemetry or new permissions. New Tab/Home and address focus retain controls. Accessibility-driven scrolling without touch input leaves controls visible. Complex nested pages and other OEMs still need acceptance.
- `core/BrowserPreferences.kt`: app-private preference, bookmark and site-exception persistence; WebView CookieManager toggles. The request-thread blocking switch and each tab's top-level page URL use volatile snapshots rather than reading Compose mutable state from WebView background callbacks.
- `core/SiteExceptions.kt`: exact-host, HTTP(S)-only per-site filter exceptions with no automatic subdomain exemptions.
- `core/BrowsingLibrary.kt`: bounded, app-private, optional recent/most-visited website host index. Records hostname, visit count, and timestamp only; never persists URL path, query, fragment, or page title. Clear/pause controls are in Privacy Center; no cross-device sync.
- `core/SiteIcons.kt`: app-private, per-host favicon cache using only WebView-reported page icons, not external favicon collection services. Files are size and count bounded and user-clearable.
- `core/Webspaces.kt`: organizational Webspaces definitions, explicit exact-host assignments, built-in provider routing, and bounded persisted custom spaces. **Not isolated Android WebView storage or authentication sessions.** See [Webspaces preview](WEBSPACES.md).
- Android WebView is the system website renderer, **not** an independently maintained browser engine.
- Browser app data is app-private. The manifest disables Android Auto Backup, and `res/xml/data_extraction_rules.xml` excludes all browser data from Android 12+ cloud backup and device-to-device transfer. Both platform and OEM behavior still require device validation.

## Key trust boundaries

- Remote pages are untrusted. No JavaScript bridge is injected, no JavaScript auto-popup windows, and no file/content access is allowed.
- Only HTTP(S) inputs are recognized as browser destinations. Explicit HTTP links are upgraded to HTTPS before loading. HTTP main-frame navigations are intercepted and upgraded when WebView exposes them as GET; altered non-GET requests are canceled rather than replayed as GET. Android's cleartext-traffic restriction remains enabled as defense in depth. TLS failures are canceled without HTTP fallback; mixed content and dangerous schemes are blocked. HTTPS-first can break legacy HTTP-only sites.
- Safe Browsing is enabled. WebChrome permission requests are denied by default pending an explicit permission UX.
- The default subscriptions are OISD Small and StevenBlack Unified; the source catalog also offers opt-in OISD Big, OISD NSFW Small, and StevenBlack Gambling. The `DomainFilterParser` accepts only the documented OISD domainswild2 and StevenBlack hosts syntaxes. The `FilterListEngine` downloads over HTTPS, checks HTTP response and content length, validates non-empty content, parses with a bounded rule count, atomically caches per-source files in app-private storage, keeps last-good data on errors and never performs network I/O during WebView request interception. Domain rules are applied to subresources only; not cosmetic/scriptlet filtering. OISD Big replaces Small to avoid superset duplication.
- The URL cleaner removes only documented tracking parameter names and preserves unrelated query fields, fragment IDs and ordering. Some redirects may not be intercepted by WebView; there is no guarantee every tracking link is removed.
- CookieManager has platform-wide WebView semantics inside the app process. Third-party cookie preferences are applied per WebView. Clearing cookies is global for this app's WebView store, not per site.
- Android WebView performs networking using the device resolver. DNS management is a settings launch only, not an in-app DNS engine.

## Security and release backlog

1. Expand the initial OISD/StevenBlack subscription engine with per-site exception UX testing (implemented as an exact-host toggle), richer filter syntaxes where technically feasible, offline startup testing and performance measurements.
2. Implement accurate per-site cookies, site permission prompts, downloads and safe file upload policy.
3. Review browser role handling, deep links, unobserved redirect classes, local-network access and threat-model regressions; the initial HTTPS-first upgrade is implemented and unit-tested but not a complete WebView security audit.
4. Implement persistent tab sessions, true private-mode boundaries and stronger history retention controls. The initial user-disableable/clearable website-host visit index is not a complete browsing-history implementation.
5. Complete accessibility, screen-reader, contrast, landscape, keyboard, dark/light and real-device WebView testing.
6. Add integration/instrumentation tests and dependency/provenance scans; verify signing and APK delivery through the approved Drive Artifacts location.

## GC UI

Visual composition adopts the governing GoreeCloud UI principles: Canvas / Surface / Lens hierarchy, Iris semantic palette inspiration, 12/18/26/36 corner progression, 4dp spacing rhythm and 44dp+ touch actions. The bottom browser navigation is a separate elevated, inset, safe-area dock with a neutral New Tab icon; overflow is a compact rounded action palette with grouped tasks. Home leads with dynamic sites and source-reported favicons; New Tab has a reduced editorial hero. Both omit the former top privacy badge and oversized Home banner. The header and adaptive launcher now consume verified PNG exports of the founder-approved Prism Tabs v5 master in [brand-assets at 1b12ecb](https://github.com/GoreeCloud/brand-assets/commit/1b12ecbb043c89b4f461e68ce2cd83b5fd70eb3f). Source identities and raster integrity are checked before Gradle CI. Android device testing confirmed the color icon in the header and OS App Info; themed monochrome and other OEM masks remain acceptance gates. See [branding provenance](BRANDING.md).

## Screenshot-directed immersive chrome candidate (November 6, 2026)

The founder's supplied *second* screenshot is the target: no redundant browser logo/name above the address bar; a premium pill search/address control immediately below the status bar; a distinct gradient circular Go action; and a lower, elevated floating nav pill with webpage content scrolling underneath it. This is source-implemented on a review branch; do not describe it as released. Before merging, validate native Android 16 visual screenshots and upward/downward scroll, keyboard/focus, light/dark, enlarged fonts, landscape and Android Back/tab behavior. Existing system status/navigation bars remain OS-owned and visible. No image generation was used.
