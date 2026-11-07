# Staggered Tab Overview — native Android preview

**Scope:** This replaces only the tab-button *list sheet* with a full-screen, Pinterest-inspired masonry overview. It does not implement multiple Android browser windows, disk-persisted tab sessions, or remote screenshot storage.

## Interaction

- Press the existing count-bearing tab button in the floating bottom dock to open the overview, not a draggable bottom sheet.
- The overview shows **all currently open tabs** (the application currently limits a session to 12) in a responsive, two-column staggered grid on a typical phone. The grid adapts to narrow screens, tablets and available space.
- Every tab has a visual preview surface, title, display-only hostname or internal page identity, Webspace label, and a separate **Close [tab name] tab** button.
- Tapping the card switches to the selected tab; close removes only that tab without opening it. The active tab has an accent border and an Active marker.
- An unobtrusive New button creates another tab; it is disabled when the 12-tab limit is reached. Back/Return exits without navigating. Closing the final tab resets it to a blank New Tab, preserving the existing last-tab behavior.
- **Reopen closed tab** appears underneath the overview heading for at most 30 seconds after closing one eligible HTTPS webpage. It loads the saved URL in a new WebView (or reuses the sole blank New Tab when appropriate) and closes the overview. It is one-shot and retains only the most recently closed page, not an archive. Closing an internal Home/New Tab or an unsafe/nonweb page clears recovery. When the tab limit is reached or the window expires, reopening is unavailable.
- **Duplicate tab** is an explicit More options action for the current HTTPS webpage while under the 12-tab limit. It opens the same address in a *new* tab and keeps Webspace grouping and text zoom, but it does **not** clone DOM, form fields, POST requests, WebView navigation history, screenshots, or temporary find queries. Desktop UA overrides start at the new tab's normal mobile default.


## Source and privacy contract

For loaded websites, `TabPreviewCapture` draws the **already attached/visible** Android WebView viewport into a 320×480 RGB_565 bitmap on the UI thread when opening the overview or switching away. Bitmaps live only in the `BrowserTab` Kotlin state and are released when the tab is closed or a new page begins loading. They are **not written to files, synced, exported, indexed in browsing history, sent to an analytics endpoint, or persisted over app restarts**. No background loading or navigation of inactive tabs is performed to obtain a screenshot.

WebView's rendering backend may refuse or fail to paint certain protected/hardware-composited content. Such cards display a clearly labelled **Preview unavailable** fallback, rather than a fabricated website screenshot. Built-in Home and New Tab cards use original native presentation placeholders, not webpage screenshots. Website display addresses are hostname-only to avoid displaying tracking tokens or query parameters in the tab picker.

## Closed-page recovery privacy contract

One eligible validated HTTPS URL (maximum 2,048 UTF-16 code units), its bounded title and Webspace identifier, and an Android monotonic close timestamp are retained **only in application RAM** for a maximum of 30 seconds. No on-disk history, session restore, background sync, thumbnail, cookie export, or analytics entry is created by recovery. Closing another tab replaces or clears the record. Choosing Reopen consumes it, and app termination discards it. The address can still contain query strings or website tokens; recovery intentionally reloads the original URL rather than cleaning it in ways that could break the page. Users needing immediate forgetting can close an internal tab or exit the app, but this is **not** an isolated private browsing mode. Do not claim full form or session recovery.

## Validation and remaining gates

The feature is native Jetpack Compose (`ui/TabOverview.kt`) and uses the existing WebView tab lifecycle and Kotlin state in `MainActivity.kt`. Validate `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`, real-device screenshots with loaded and unloaded tabs, card tap/close behavior, last-tab recovery, count update, Android Back handling, memory growth across 12 tabs, orientation/large fonts, TalkBack focus and accessibility, and secure-page previews. A passed build is not visual accessibility or Stable release approval.

The Prism Tabs v5 icon is unchanged; the feature uses the existing GoreeCloud UI palette and floating dock. No image generation was used.
