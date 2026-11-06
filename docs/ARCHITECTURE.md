# Architecture & risk boundaries

## Components

- `MainActivity.kt`: native Compose toolbar, Home, New Tab, tabs, privacy settings, Android WebView host.
- `core/NavigationPolicy.kt`: HTTPS-first address resolution, upgraded HTTP navigation without downgrade fallback, non-GET rewrite rejection, search fallback, tracking parameter normalization and starter domain blocklist.
- `core/BrowserPreferences.kt`: app-private preference, bookmark and site-exception persistence; WebView CookieManager toggles. The request-thread blocking switch and each tab's top-level page URL use volatile snapshots rather than reading Compose mutable state from WebView background callbacks.
- `core/SiteExceptions.kt`: exact-host, HTTP(S)-only per-site filter exceptions with no automatic subdomain exemptions.
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
4. Add persistent tab sessions, site history with private-mode boundaries and user-controlled retention.
5. Complete accessibility, screen-reader, contrast, landscape, keyboard, dark/light and real-device WebView testing.
6. Add integration/instrumentation tests and dependency/provenance scans; verify signing and APK delivery through the approved Drive Artifacts location.

## GC UI

Visual composition adopts the governing GoreeCloud UI principles: Canvas / Surface / Lens hierarchy, Iris semantic palette inspiration, 12/18/26/36 corner progression, 4dp spacing rhythm and 44dp+ touch actions. Precise official native token parity and actual rendered visual acceptance have **not** yet been verified.
