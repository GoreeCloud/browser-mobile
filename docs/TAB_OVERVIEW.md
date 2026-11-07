# Staggered Tab Overview — native Android preview

**Scope:** This replaces only the tab-button *list sheet* with a full-screen, Pinterest-inspired masonry overview. It does not implement multiple Android browser windows, disk-persisted tab sessions, or remote screenshot storage.

## Interaction

- Press the existing count-bearing tab button in the floating bottom dock to open the overview, not a draggable bottom sheet.
- The overview shows **all currently open tabs** (the application currently limits a session to 12) in a responsive, two-column staggered grid on a typical phone. The grid adapts to narrow screens, tablets and available space.
- Every tab has a visual preview surface, title, display-only hostname or internal page identity, Webspace label, and a separate **Close [tab name] tab** button.
- Tapping the card switches to the selected tab; close removes only that tab without opening it. The active tab has an accent border and an Active marker.
- An unobtrusive New button creates another tab; it is disabled when the 12-tab limit is reached. Back/Return exits without navigating. Closing the final tab resets it to a blank New Tab, preserving the existing last-tab behavior.

## Source and privacy contract

For loaded websites, `TabPreviewCapture` draws the **already attached/visible** Android WebView viewport into a 320×480 RGB_565 bitmap on the UI thread when opening the overview or switching away. Bitmaps live only in the `BrowserTab` Kotlin state and are released when the tab is closed or a new page begins loading. They are **not written to files, synced, exported, indexed in browsing history, sent to an analytics endpoint, or persisted over app restarts**. No background loading or navigation of inactive tabs is performed to obtain a screenshot.

WebView's rendering backend may refuse or fail to paint certain protected/hardware-composited content. Such cards display a clearly labelled **Preview unavailable** fallback, rather than a fabricated website screenshot. Built-in Home and New Tab cards use original native presentation placeholders, not webpage screenshots. Website display addresses are hostname-only to avoid displaying tracking tokens or query parameters in the tab picker.

## Validation and remaining gates

The feature is native Jetpack Compose (`ui/TabOverview.kt`) and uses the existing WebView tab lifecycle and Kotlin state in `MainActivity.kt`. Validate `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`, real-device screenshots with loaded and unloaded tabs, card tap/close behavior, last-tab recovery, count update, Android Back handling, memory growth across 12 tabs, orientation/large fonts, TalkBack focus and accessibility, and secure-page previews. A passed build is not visual accessibility or Stable release approval.

The Prism Tabs v5 icon is unchanged; the feature uses the existing GoreeCloud UI palette and floating dock. No image generation was used.
