# GoreeCloud Browser Mobile — user mockup and New Tab audit

**Review date:** November 6, 2026  
**Source:** A founder-provided New Tab UI mockup in the active project conversation. The uploaded screenshot is a *reference*, not a product build or claim of implemented functionality. It is not copied into this public repository.  
**Scope:** Native Android Compose New Tab page, developer preview, one physical OnePlus N200 / Android 16.

## Mockup-derived visual requirements

The supplied mockup depicts a branded browser header and address bar, a lightweight blue wave near the headline, prominent "Where to next?" messaging, a slim Startpage discovery card, four Quick Access actions (Bookmarks, History, Downloads, Settings), and a floating bottom browser toolbar.

The implementation preserves that composition. The wave is **drawn in native Compose Canvas paths** with subtle Iris/Lagoon tint; no generated image, raster screenshot or external artwork is used. The current first-party icon uses the central [Browser Mobile identity](https://github.com/GoreeCloud/brand-assets/tree/main/products/browser-mobile).

## Changes made during the pre-finalization audit

| Mockup or inherited issue | Decision / rationale |
| --- | --- |
| Mockup includes a "Private" header badge | Omitted because the founder previously explicitly requested its removal. The Privacy Center remains accessible from Settings and overflow; badge should not imply full tracking or session isolation |
| Mockup shows a blue active underline beneath the plus button | Not recreated: the founder previously asked for a neutral, unhighlighted New Tab button |
| Decorative wave touches descriptive text | Made translucent, limited to the right of the hero, and moved further away from the subtitle after physical-device review |
| Large CTA/promotional cards could repeat the address bar | Single compact provider card labeled "Explore the web — Powered by Startpage"; its Open action navigates to the fixed Startpage home, not an unverified GoreeCloud Search default |
| Four Quick Access buttons can be too narrow at larger font sizes | Uses 4 columns at default phone width; changes to 2×2 when effective content width is under 340dp or font scaling is at least 120% |
| Some mockup features do not yet exist as complete browser subsystems | Every shortcut has a functioning, bounded action; none is a dead decorative control. History is explicitly **website-host-level**, and Downloads delegates to Android Downloads instead of pretending to provide a full browser download manager |
| Privacy/history overcollection risk | Existing BrowsingLibrary stores hostname, visit count and timestamp only; no page paths, search terms, or fragments. Privacy Center can disable and clear this index |
| Visual layers risk covering browser controls | Floating navigation dock remains separate and lower than the scrolling New Tab content, with reserved page space and safe-area padding |
| Placeholder branding | Uses the officially authored Browser Mobile portal vector and shared GoreeCloud UI color direction, not letter placeholders |

## Shortcut behavior and limitations

- **Bookmarks:** Displays locally saved full URLs (30-item limit), opens a saved URL, and supports removing bookmarks. Empty state describes how to save.
- **History:** Displays up to 100 visited website hosts ordered by recency; each row returns to the root of that HTTPS host. This is not a page-by-page browsing journal. Users can clear it.
- **Downloads:** Opens a transparent explanatory panel and offers **Open device downloads**, which launches Android's Downloads activity when available. Browser Mobile does not yet implement full file-download management.
- **Settings:** Opens the current Privacy Center including adblocking, cleaner, history, cookies and the Android Private DNS shortcut.

## Device audit (November 6, 2026)

The authorized OnePlus N200 running Android 16 was used for a **debug-only, nonproduction** smoke test. Direct screenshot review verified the New Tab hero, lightweight wave, compact Startpage card, header, unhighlighted New Tab action, floating dock, four Quick Access tiles, and native page actions. At 130% system font scale, the shortcut layout became 2×2. After returning to 100% and restarting the app, it became a four-column row. Bookmarks empty state and real host-history results were observed. The Downloads panel opened, and Android resolved and launched its `com.android.documentsui/.ViewDownloadsActivity` when the panel button was pressed. The Settings shortcut opened the existing Privacy Center, and the provider card's Open action loaded `https://www.startpage.com/` in the WebView. One history-label singular/plural error and the wave/text proximity were corrected during review.

**Final local verification:** The final audited APK was rebuilt with `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` and passed **26/26 unit tests (zero failures/errors), APK assembly and Android Lint**. Android v2 APK signature verification succeeded; the local debug APK SHA-256 is `da72dd521ef2821e1b63942635b1ffde79b13a03074c6a366babdf17ad716ba0`. After reinstalling, light and dark screenshots confirmed the wave was moved clear of the subtitle; 100% text displays four tiles in one row, while 130% text uses 2×2; portrait and landscape remained usable with scrolling for content below the landscape fold. The device's original light theme, portrait rotation and 100% text setting were restored. GitHub CI remains a separate post-commit verification gate.

**Further acceptance gates:** Full TalkBack and hardware-keyboard accessibility, OEM/device matrix, performance, genuine in-browser downloads, full page-by-page history, other browser security/privacy work, production signing and Store distribution. An Android 16 preview device test is not Stable acceptance.

No image generation was performed. The user-provided design is the source reference, and implementation choices/limitations above are explicit rather than silently substituted.
