# GoreeCloud Browser Mobile — Webspaces Preview

**Date:** November 6, 2026  
**Status:** Android tab organization/routing preview. Not isolated Firefox containers or Stable.

## Reference

The existing [GoreeCloud Webspaces Firefox extension](https://github.com/GoreeCloud/firefox-addons/tree/main/extensions/webspaces) is the functional reference. Its six built-ins use Firefox contextual identities and distinct cookie stores. **Android WebView does not expose an equivalent per-tab contextual cookie store.** Do not confuse a Webspace label or an assigned tab with identity, session, account or site-data isolation.

## Implemented mobile scope

- Built-in spaces: **Standard, GoreeCloud, Google, Microsoft, Meta, Proton**.
- Up to **10 user-created** named custom spaces, persisted in app-private preferences. User can remove custom spaces; affected in-memory tabs revert to Standard.
- Local, deterministic provider-domain routing with suffix-boundary matching. Unassigned websites enter Standard. Unsupported schemes are not used for site routing.
- Up to **100 explicit exact-host assignments**, overriding provider defaults. Site rule creation and removal are in the Webspaces panel. Rules do not automatically match subdomains; broad provider rules do.
- Each tab carries a mutable Webspace label. Users can open a space, move a current tab into a space, or route an exact website to a space. A manually moved tab remains pinned to its Webspace until the user changes it.
- Tab-group and routing configuration never includes cookies or credentials in its JSON preferences. All browser tabs still share Android WebView's app-level cookies and web storage.
- Pop-up overflow menu provides the Webspaces manager. The user can see space names and counts; unverified claims of isolation are explicitly disallowed.

## Deliberate limits and next steps

- **No independent authenticated sessions, Firefox containers, cookie-store separation, per-Webspace private browsing, separate WebView profiles, or per-space DNS/proxy/VPN.** A site may remain logged in to the same account across spaces.
- No import/export, locks, temporary Close & Forget, session migration, sophisticated routing pause, or signed Firefox extension parity yet.
- Tab memberships are in memory; Webspace definitions and routing rules persist. On app restart new tabs begin in Standard unless a URL is routed.
- Before claiming account isolation, evaluate Android's supported WebView data-directory/process architecture, lifecycle, security boundaries and test independently. Never simulate isolation using only UI labels.
- Provider marks for the mobile Webspaces manager are not yet packaged. Generic, accessible icons are not claims of provider affiliation.
- The shared GoreeCloud Browser family icon has since been approved as **Prism Tabs v5** and centrally promoted in [brand-assets](https://github.com/GoreeCloud/brand-assets/tree/main/products/browser-mobile); Browser Mobile uses its source-pinned Android header and launcher exports. The original Webspaces feature preview still has generic space glyphs, which are not provider trademarks or isolated profiles. See [branding status](BRANDING.md).
