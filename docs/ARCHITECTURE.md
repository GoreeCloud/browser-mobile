# Architecture & risk boundaries

## Components

- `MainActivity.kt`: native Compose toolbar, Home, New Tab, tabs, privacy settings, Android WebView host.
- `core/NavigationPolicy.kt`: HTTPS-or-HTTP URL acceptance, search fallback, tracking parameter normalization and starter domain blocklist.
- `core/BrowserPreferences.kt`: app-private preference and bookmark persistence; WebView CookieManager toggles.
- Android WebView is the system website renderer, **not** an independently maintained browser engine.

## Key trust boundaries

- Remote pages are untrusted. No JavaScript bridge is injected, no JavaScript auto-popup windows, and no file/content access is allowed.
- HTTP and HTTPS URLs are the only browser navigations. Explicit dangerous schemes are rejected. TLS failures are canceled; mixed HTTP/HTTPS content is disabled.
- Safe Browsing is enabled. WebChrome permission requests are denied by default pending an explicit permission UX.
- The blocked-domain list applies to subresource requests, not top-level navigation. Its default coverage is intentionally conservative and static.
- The URL cleaner removes only documented tracking parameter names and preserves unrelated query fields, fragment IDs and ordering. Some redirects may not be intercepted by WebView; there is no guarantee every tracking link is removed.
- CookieManager has platform-wide WebView semantics inside the app process. Third-party cookie preferences are applied per WebView. Clearing cookies is global for this app's WebView store, not per site.
- Android WebView performs networking using the device resolver. DNS management is a settings launch only, not an in-app DNS engine.

## Security and release backlog

1. Implement a maintained, versioned adblock filter engine with syntax/false-positive tests and exception controls.
2. Implement accurate per-site cookies, site permission prompts, downloads and safe file upload policy.
3. Review browser role handling, deep links, redirects, local-network access, HTTPS-only mode and threat-model regressions.
4. Add persistent tab sessions, site history with private-mode boundaries and user-controlled retention.
5. Complete accessibility, screen-reader, contrast, landscape, keyboard, dark/light and real-device WebView testing.
6. Add integration/instrumentation tests and dependency/provenance scans; verify signing and APK delivery through the approved Drive Artifacts location.

## GC UI

Visual composition adopts the governing GoreeCloud UI principles: Canvas / Surface / Lens hierarchy, Iris semantic palette inspiration, 12/18/26/36 corner progression, 4dp spacing rhythm and 44dp+ touch actions. Precise official native token parity and actual rendered visual acceptance have **not** yet been verified.
