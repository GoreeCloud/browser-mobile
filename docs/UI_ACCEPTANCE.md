# GoreeCloud Browser Mobile — UI acceptance review

**Reviewed:** November 6, 2026  
**Target:** Native Android Compose preview, OnePlus N200 (DE2117), Android 16 (API 36), 1080×2400 portrait, landscape, light and dark appearance.  
**Scope:** Bounded source, build, device screenshots and interaction smoke tests; **not** Android-wide, assistive-technology, production-release, or Stable approval.

## Requested corrections and observed evidence

| User requirement | Implementation and evidence | Status |
| --- | --- | --- |
| Replace unconvincing launcher icon | New first-party browser-portal symbol with frame/chrome dots/directional aperture replaces prior G glyph; adaptive foreground, themed monochrome, header vector and platform app-info screenshot checked. Canonical original icon, transparent symbol and light/dark wordmarks: [GoreeCloud/brand-assets at 0239de5](https://github.com/GoreeCloud/brand-assets/commit/0239de5e73683aec55454b1963b4f3dca593a14c). | Verified on one device; store/other OEM masks unverified |
| Un-highlight the New Tab button | All six dock buttons use a neutral visual treatment; no active gradient special-case for New Tab. | Verified light/dark screenshots |
| Floating navigation | Dock is an independent, bottom-centered, inset Compose overlay with a rounded frame, shadow, border, safe-area separation, and reserved page-content space. | Verified portrait and landscape |
| Better three-dot menu | Compact 262dp wide, 22dp-radius contextual action palette with small icon tiles and quick Webspaces/Privacy/Home rows; old draggable bottom menu removed. Disabled page actions remain labeled. | Verified screenshot in light; full keyboard/TalkBack pending |
| Reduce visual clutter on New Tab | Replaced oversized gradient-add hero and duplicated app emblem with smaller editorial headline and search/discovery content. | Verified device screenshot |
| Reorder Home for usefulness | Quick Destinations appear first, with Recent/Favorites/Most Visited (up to eight per view), source-provided favicons and a neutral globe fallback; privacy statistics follow. | Verified actual device history and Wikipedia favicon |
| Clear, honest privacy stats | URL-cleaner count is labeled URL fields cleaned, not generic trackers removed. | Source reviewed, visual check pending |
| Text scaling | At 130% font, address placeholder was observed to wrap excessively; changed to a one-line, ellipsized shorter prompt. The later founder-mockup New Tab uses a 2×2 Quick Access grid at 130% to preserve labels. | Fixed and screenshot-verified on Android 16 |

## Architecture boundaries

Webspaces remain a **tab grouping and routing preview**, not isolated cookie stores or signed-in identities. Default search remains Startpage until GoreeCloud Search is independently Stable. The redesigned app retains the existing domain-based filters, exact-host exceptions, URL cleaner and Android OS Private DNS shortcut. None of this UI review certifies browser-security completeness.

## Build and provenance

The debug build uses Android SDK 35 and JDK 21. Regression suite: **26 JVM tests passed, zero failures/errors** at the previous verified run; Android Lint completed without fatal errors. Native launcher source paths are manually mapped counterparts of the first-party central SVG master; they are not byte-identical SVG exports. The central brand-assets CI passes for source-integrity/vector checks at the referenced commit.

No AI image-generation tool was used. Original hand-authored source vectors were created as text files in the brand repository. Device screenshots include OS status information and are retained only on the authorized development computer, not uploaded into the public GitHub repositories.

## Non-approved / next acceptance gates

- Full accessibility: TalkBack, large text above 130%, switch/keyboard focus order, dynamic colors, high contrast and reduced motion.
- Smaller/large-form-factor Android devices, various OEM launcher masks, Android API 26–35, foldables and tablets.
- Complete website workflows, private tabs and true Webspace identity isolation, site permissions, download handling, and extensive UI performance testing.
- Production signing, Play listing, Store artwork and device-backed stable-release certification.

**Approval meaning:** The named fixes may be accepted for the *development preview* only after final local build/CI and device checks. This document does not declare all possible UI problems solved or authorize Stable release.

## November 6 founder New Tab mockup — follow-on implementation

The founder supplied an updated New Tab mockup and asked for an audit before finalization. The resulting native Compose screen adopts its compact Startpage card, wave-accented hero and four working Quick Access controls. [The detailed source-grounded mockup audit](NEW_TAB_MOCKUP_AUDIT.md) records why the earlier founder-requested **removal of the Private badge** and **unhighlighted New Tab button** were retained even though the newer mockup visually included both.

On the authorized OnePlus N200, the final local debug APK installed and rendered in portrait, landscape, light/dark and both 100% and 130% font scale. Default typography shows four Quick Access tiles horizontally; enlarged text uses a two-column layout. Bookmarks, host-only History, Android Downloads and Settings panels were opened, and the fixed Startpage Open action loaded `https://www.startpage.com/` in WebView. The device's light theme, portrait orientation and 100% font settings were restored after the smoke tests.

The final local build passed 26 JVM tests with zero errors/failures, Android Lint, debug APK assembly and Android v2 signature verification. This is **development-preview source/device verification**, not whole-browser UI approval, a complete accessibility audit or Stable release.

## November 6 — premium New Tab and identity rework after founder rejection

Source improvements now include a simplified open-ring/forward-arrow vector (header, adaptive launcher and monochrome), canonical central brand-assets revision [e02c3d5](https://github.com/GoreeCloud/brand-assets/commit/e02c3d5), a raised search field, grouped editorial New Tab hero, subtle native orbit ambience, an elevated navy Startpage feature card and tonal Quick Access tiles. Up to two real visited hosts appear beneath the shortcuts when app-private website-host history is enabled. The founder-requested neutral New Tab action and removal of the top privacy badge were preserved.

A debug build was installed on the authorized OnePlus N200 and visually inspected in light/dark and at 100%/130% text. In the initial screen review, the dot/eyebrow floated too far from the heading; grouping those elements into one hero was the final polish correction. No AI image generation occurred. This is source-and-device preview validation, **not founder visual approval or comprehensive accessibility acceptance**.

**Second-generation verification after final hero spacing fix:** local Gradle `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` **BUILD SUCCESSFUL**, with **26 JVM tests passed** and zero failures/errors. Locally debug-signed APK SHA-256: `b98f9dc82ed5e29844569171fc83a77d04f7faf632e94e1bfe5df510fd42a710`; `apksigner verify` confirms Android v2 signing. The revised APK installed in place on the authorized OnePlus N200, and portrait screenshots were checked after launch fade. Landscape remains scrollable but requires scrolling to reach lower cards; short-height landscape optimization remains open. The phone's light theme, 100% font scale and portrait rotation were restored. Central brand-assets source at [e02c3d5](https://github.com/GoreeCloud/brand-assets/commit/e02c3d595d046e426bdf94882557a8d559da27c6) passed source-integrity and vector checks and both verified GitHub CI runs. Android browser CI must still be verified at its own final commit.

## November 6 — founder screenshot correction: immersive address and navigation (review candidate)

**Target:** The founder's second supplied screenshot, not the first. Remove the redundant icon/name header from the browser's content region. Use a single premium rounded address pill beneath the Android status bar, with its own tonal secure/search glyph, legible editable URL text, and distinct circular gradient Go button. Keep a raised floating bottom dock *above* the Android navigation area while allowing real webpage pixels to continue under it.

**Source behavior under review:** Native Android WebView vertical scrolling downward progressively hides both app chrome bars after a direction-sensitive threshold; scrolling upward or back to page top reveals them. Address-field focus, explicit navigation, switching tabs and opening the overview restore controls. Home and New Tab retain controls without scroll auto-hide. Native WebView scroll callback is used (no JavaScript injected into webpages), with `core/BrowserChromeScrollPolicy.kt` JVM tests; nested independently scrolling web elements may need future compatibility work.

**Required acceptance before Stable or full visual approval:** compile, JVM tests, Android Lint; physical Android 16 screenshots of top/scroll-down/scroll-up and an editable address; ensure the full webpage can reach its bottom without obstruction; check light/dark, font 100/130%+, landscape, navigation/tab manager/menus, device Back, and TalkBack. Screenshot appearance alone cannot prove gesture behavior. This section records the requested design and source candidate, **not** completed device validation or deployment.
