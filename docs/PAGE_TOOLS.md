# Page tools — Find in Page and Text Size

## Owner's intent and scope

I want GoreeCloud Browser Mobile to offer polished, understandable controls for finding content on a webpage and making webpage text more readable, without sending private searches to a search provider or retaining a user's reading preferences as a site-level profile. These features use the native Android WebView of the **current tab** and the existing GoreeCloud UI action palette. This document describes a development preview, not Stable acceptance.

## Find in Page

- **Entry:** On a website, choose **More options → Find in page**. The floating address bar remains visible and a compact in-page search strip appears below it.
- **Behavior:** Typing invokes WebView's built-in asynchronous `findAllAsync` on the active document. A WebView find listener reports the active ordinal and count. Accessible **Previous match**, **Next match**, and **Close find in page** controls provide navigation and exit. Android Back first dismisses the find interface rather than navigating away, when the IME has not consumed Back.
- **Privacy:** The query lives only in the current in-memory tab; it is not added to visited-host history, bookmarks, shared preferences, telemetry, remote requests or the Startpage provider. No JavaScript or DOM injection is used. The query is bounded to 160 UTF-16 code units, without splitting a surrogate pair at the boundary. A blank query clears highlights. Closing the strip or starting a navigation clears the query and WebView find highlights. Closing a tab destroys its WebView and all transient page-tool state.
- **Limits:** WebView search operates on text available to its find implementation. It is not a cross-tab search, search-engine query, PDF search, or universal reader-mode parser. Dynamic websites may update matches after the original find operation.

## Text Size

- **Entry:** On a website, choose **More options → Text size**.
- **Behavior:** **Smaller**, **Larger**, and **Reset text size to 100%** act on `WebView.settings.textZoom`. Initial value is 100%; adjustments use 20-percentage-point steps and clamp between 80% and 200%. The toolbar clearly displays the current percent. The current tab remembers this in memory when switching tabs.
- **Privacy:** No per-domain preference, cookie, local database record, analytics event, server request or permission change is made. Other tabs retain their independent initial/adjusted zoom values.
- **Limits:** Some websites specify viewport/font rules that may override WebView text zoom. This is **text scaling**, not pinch-zoom, desktop-site emulation, layout zoom, reader mode, accessibility-service settings or a promise of universal reflow.

## Regression and device verification

- `PageToolsPolicyTest` exercises 20% zoom steps, range enforcement including extreme values, query boundaries, Unicode surrogate pairs, and preservation of normal internationalized text.
- `PageToolsInstrumentedTest` refuses all normal application IDs, requires a disposable `.qa` target, and uses a synthetic local HTML document containing exactly three occurrences of a unique marker. It validates accessible entry, result counts, next/previous, close cleanup and live WebView text zoom increase/decrease/reset. The test does not load actual user pages or use normal browser data.
- The existing `ChromeGestureInstrumentedTest` and `PrivacyConfirmationInstrumentedTest` remain part of the full Android device suite. Actual passing run details belong in `docs/DEVICE_ACCEPTANCE.md` after execution, not merely after compilation.
- Android CI compiles source and instrumentation APK but does not automatically execute physical Android tests. Passing build/JVM/lint alone is not device UX, TalkBack, OEM/API, or Stable release approval.

## Remaining acceptance

Manual TalkBack navigation, physical keyboard/IME/Back behavior, 200%+ font setting, small screens, portrait/landscape/tablet/dark visual quality, interactive complex/nested pages, multilingual/RTL page text, site-specific text zoom compatibility, WebView/OEM variations, and extended gestures require further independent checks. No production release or image generation is implied.
