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

## November 6 — second-generation premium refinement following founder feedback

The founder rejected the previous Android New Tab appearance and mini browser-window logo even after the initial mockup implementation. Rather than relabel that design as approved, the source and its canonical identity were revised. This review targets the **early development preview**.

**Changed:** the header now uses a clearer 46dp first-party open-ring/forward-arrow mark, redesigned consistently in Android color, adaptive foreground and themed monochrome vectors. The [canonical SVG icon and wordmarks](https://github.com/GoreeCloud/brand-assets/tree/main/products/browser-mobile) were revised at `e02c3d5` with source identity checks and SVG validation. No image generator or licensed external mark was used.

The New Tab uses a more cohesive typographic hierarchy: eyebrow, headline and supporting text grouped as a single hero rather than detached; a quiet native Canvas radial-orbit ambient; a navy-to-deep-blue feature surface with a bright Startpage action; restrained Iris/Lagoon tonal shortcut wells and reduced card shadows; and a conditional **Pick up where you left off** section showing up to two genuinely visited hosts and WebView-supplied favicons. That optional row disappears if host-level history is off or empty. The search bar now uses a subtle raised surface, framed go action and readable 14sp prompt. The floating dock and neutral New Tab button were retained, as were the distinct Home page and Startpage-only search policy.

**Visual and functional evidence:** The revised debug APK installed on a connected OnePlus N200 and was inspected in portrait light/dark mode. The new mark remained clearly legible at header size; original screenshots were retained only on the authorized device. At 130% system text, shortcut cards became a 2×2 grid with no observed label clipping. Website shortcut previews used locally cached favicons, not external icon services. The initial build compiled, assembled a debug APK, ran 26 JVM tests and Android Lint successfully. After noticing excess vertical whitespace between the eyebrow and headline, the hero was regrouped and a final build was started. This paragraph does **not** claim that the founder has approved the aesthetic.

**Still required for broad acceptance:** full TalkBack and keyboard testing, all Android API/OEM/launcher form factors, external-asset parity, thorough contrast/zoom/a11y matrix, private browsing security and real download management, production signing and founder visual feedback. The redesign is not a Stable browser.

**Second-generation verification after final hero spacing fix:** local Gradle `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` **BUILD SUCCESSFUL**, with **26 JVM tests passed** and zero failures/errors. Locally debug-signed APK SHA-256: `b98f9dc82ed5e29844569171fc83a77d04f7faf632e94e1bfe5df510fd42a710`; `apksigner verify` confirms Android v2 signing. The revised APK installed in place on the authorized OnePlus N200, and portrait screenshots were checked after launch fade. Landscape remains scrollable but requires scrolling to reach lower cards; short-height landscape optimization remains open. The phone's light theme, 100% font scale and portrait rotation were restored. Central brand-assets source at [e02c3d5](https://github.com/GoreeCloud/brand-assets/commit/e02c3d595d046e426bdf94882557a8d559da27c6) passed source-integrity and vector checks and both verified GitHub CI runs. Android browser CI must still be verified at its own final commit.
