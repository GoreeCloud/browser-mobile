# GoreeCloud Browser Mobile

**Native Android browser · early development preview · MIT-licensed**

GoreeCloud Browser Mobile is an original, privacy-oriented Android browser built in Kotlin with Jetpack Compose and the Android system WebView. The browser chrome is native; websites are rendered using Android WebView. It is **not** a VPN, a private DNS resolver, or a mature replacement for a security-reviewed browser.

## Preview features

- **Two deliberately different starting surfaces:** a fresh **New Tab** discovery canvas and a personalized **Home** view opened with the Home button.
- **Startpage search by default**, with a fixed, documented policy that prevents premature migration to GoreeCloud Search.
- **Native tabs** (up to 12 per session), back/forward, address/search field, reload, web sharing, and lightweight bookmarks.
- **URL cleaning** for a conservative set of known advertising/campaign parameter names. Necessary non-tracking query parameters are preserved.
- **Built-in ad/tracker domain subscriptions**: OISD Small and StevenBlack Unified are **enabled by default**. The user can opt into OISD Big (replaces Small), OISD NSFW Small, or StevenBlack Gambling; disabled lists do not download. Lists are fetched only over HTTPS, updated at app startup when older than 24 hours, locally cached with last-known-good fallback, and manually refreshable. A reversible **Allow ads on this site** switch pauses request filtering for that exact top-level site; other sites remain protected. This provides domain-based WebView subresource filtering only—not cosmetic filtering or exhaustive ad blocking. See [filter subscription policy](docs/FILTER_SUBSCRIPTIONS.md).
- **Cookie controls** for global cookie acceptance, third-party cookies (off by default), and explicit clearing of cookie and web-storage data.
- **System DNS manager entry point** to Android Private DNS settings. Android WebView uses the OS DNS stack; per-app custom DNS/DoH is **not** implemented.
- **Modern GC UI-inspired Iris treatment** with system light/dark modes, branded launcher artwork, clear bottom navigation, and touch-friendly settings.
- Defensive WebView defaults: no file/content access, mixed-content prohibition, Safe Browsing enabled, no auto popups, explicit denial of browser device-permission prompts, and SSL-error cancellation.

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
- Bookmarks and preferences are stored locally in app-private Android SharedPreferences. Tabs and visited URLs are not synced. Browser websites can still use regular WebView storage unless cleared.
- Domain-list blocking is not equivalent to uBlock Origin: first-party/same-host ads, cosmetic placeholders, video ads and some requests are not blockable with WebView's subresource interception. The app downloads selected public lists directly from their publishers; subscriptions are unbundled and use the publishers' respective distribution terms.
- DNS settings are delegated to Android. A dedicated DNS-over-HTTPS resolver would require a separately evaluated networking architecture; exposing a DNS setting does not implement it.
- Downloads, desktop-site emulation, session restoration, granular site permissions, built-in password management, and per-site cookie deletion remain future work.
- There has not yet been a full device/browser compatibility, accessibility, privacy leak, or WebView security audit.

## Architecture and development

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for component boundaries, privacy trade-offs, and risk-based acceptance. CI compiles and runs JVM unit tests. The full APK delivery requirement is governed by GoreeCloud's canonical Google Drive instructions: installable APKs delivered for testing must be placed individually in `GoreeCloud/Artifacts/` after build provenance and identity checks, not delivered as GitHub Actions ZIPs.

## Open source

The GoreeCloud-authored source in this repository is licensed under the [MIT License](LICENSE). Public filter-list data is retrieved **separately at runtime** and remains under its owners' licenses: OISD (GPL-3.0) and StevenBlack's aggregated, multi-source hosts data (including upstream attribution/licenses). See [docs/FILTER_SUBSCRIPTIONS.md](docs/FILTER_SUBSCRIPTIONS.md). Android, Gradle, Kotlin and AndroidX are third-party components under their own licenses; GoreeCloud does not claim ownership of those dependencies or of the external list data.
