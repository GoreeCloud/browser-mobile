# Browser Mobile — Desktop site and Copy clean link

## Purpose and maturity

These are optional, local browser actions added to the Android development preview through [PR #13](https://github.com/GoreeCloud/browser-mobile/pull/13). They reuse the existing floating **More options** menu and Android WebView. They do not create new DNS, proxy, storage, account, telemetry, tracking, or permission capabilities. Neither is a substitute for a full desktop engine, comprehensive URL anonymity, or a Stable security-reviewed browser.

## Request desktop site / restore mobile site

**Entry:** while browsing a webpage, tap **More options → Desktop site**. The corresponding action becomes **Mobile site** until reverted.

**Behavior:** A new per-tab flag requests a standard desktop Chromium-style User-Agent using the *version already advertised by this installed Android WebView*. The WebView also enables wide-viewport and overview settings; these are restored to the **actual initial settings captured from that specific tab's WebView** when the user chooses Mobile site. Both changes explicitly reload the current tab. If the installed WebView has no parsable Chrome/Chromium version, desktop mode is unavailable rather than supplying an unverified browser version. No settings are persisted to SharedPreferences, cookie storage, or a hostname-based record. Other tabs retain their independent defaults. Closing the last tab resets desktop mode.

**Compatibility and privacy limits:** This is an opt-in *request*, not a desktop rendering engine. Websites can use user-agent client hints, CSS, screen width, responsive layout and feature detection; they may still serve mobile content or react incompatibly to a desktop UA. Since a desktop-style UA is intentionally sent on the next page load, website-visible fingerprint characteristics differ from normal Android WebView. It is not an anti-fingerprinting mode. Toggling reloads the tab and can prompt for form resubmission like any browser reload. Device tests are needed to validate that both directions restore exactly the OEM's original WebView behavior.

## Copy clean link

**Entry:** while browsing a webpage, tap **More options → Copy clean link**.

**Behavior:** The current tab's full HTTP(S) URL is validated using the existing `NavigationPolicy.isHttpUrl` safety checks. `UrlCleaner.clean` removes only the existing conservative list of recognized campaign/tracking query keys, retaining paths, fragments, and all non-tracking query fields verbatim. A malformed/nonweb URL or a URL with embedded credentials cannot be copied by this action. No network request, remote search, JavaScript injection, URL shortening, server-side cleanup, history entry, new Android permission, or hidden automatic clipboard write is performed. Only the explicit user tap writes an Android `ClipData` text item named `Clean page link`.

**Privacy limits:** Some URLs contain private identifiers in paths, unrecognized query fields or fragments. **Copy clean link is not proof that a URL is anonymous or safe to share.** Android's **system clipboard** is controlled by the OS and may be available to other applications and OS clipboard previews according to platform permissions; it is **not** an app-private store. Users should review sensitive links before pasting elsewhere. Existing normal share behavior remains distinct and unchanged.

## Verification and acceptance gates

The pure Kotlin `PageActionPolicyTest` covers Chrome-version preservation in desktop-UA derivation, rejecting unsupported UAs, removal of mobile model markers, conservative tracker removal and rejection of malformed/nonweb URLs. The app-specific instrumentation case checks Desktop/Mobile UI actions, actual WebView UA + viewport toggling/restoration, and that the clean-link affordance is accessible.

Physical-device QA is run exclusively under a disposable `.qa` application/runner with synthetic locally loaded HTML. It **does not tap the clipboard-copy action on the owner's personal handset**, so existing clipboard contents are not inspected or overwritten. Clipboard delivery remains a code-level/API behavior, not physically verified OS-write behavior. Development evidence belongs in [Device acceptance](DEVICE_ACCEPTANCE.md) once collected. Independent TalkBack, physical keyboard/IME, 320dp/large-font, landscape/dark, website compatibility, multiple Android versions/OEMs, secure-link sharing, and Stable release gates remain separate.
