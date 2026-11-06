# GoreeCloud Browser Mobile — Filter subscriptions

Updated: November 6, 2026. **Development preview; not a verified stable adblocker.**

## Default subscriptions

| Source | Default | Publisher URL | Format |
| --- | --- | --- | --- |
| OISD Small | On | https://small.oisd.nl/domainswild2 | Plain domains, matching subdomains |
| StevenBlack Unified | On | https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts | Hosts (adware + malware) |
| OISD Big | Off, user optional | https://big.oisd.nl/domainswild2 | Plain domains |
| OISD NSFW Small | Off, user optional | https://nsfw-small.oisd.nl/domainswild2 | Plain domains |
| StevenBlack Gambling | Off, user optional | https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/gambling/hosts | Hosts |

OISD Big contains Small, so switching on Big automatically switches off Small, and vice versa. StevenBlack Gambling includes StevenBlack Unified; overlapping domains are de-duplicated, but this optional list may block sites the user wants. The NSFW option is *off by default* and blocks domains as categorized by the list maintainer, not an app-level content safety system.

## How it works

- Users can enable or disable each list in **Privacy center → Filter subscriptions**, or disable adblocking entirely with the master switch.
- Enabled lists are downloaded directly from their publishers over TLS on the first run when blocking is active. Only fixed, reviewed HTTPS endpoints are supported; arbitrary remote list URLs are not yet accepted.
- The app checks for updates at startup if the cached list is at least 24 hours old; **Update enabled lists now** starts an explicit refresh. No independent unattended update process is configured.
- Downloads use bounded network and file sizes, a strict HTTP 200 response, limited parser syntax, a minimum parsed rule count, and atomic replacement of a previous successful cache.
- If the download fails, the last valid locally cached list remains active. Until any list is successfully loaded, a minimal built-in starter set of domains provides limited fallback. The UI shows whether each selected list has actually loaded.
- Only HTTP(S) **subresource** hosts in the Android WebView are matched; main-frame navigation is not blocked by the filter engine. Rules use exact domain or dot-boundary subdomain matching.
- The app does **not** intercept or modify operating-system DNS, TLS, VPN traffic, other apps, first-party page scripts, or cosmetic page elements. It cannot guarantee blocking every ad or tracker.
- All downloaded list files are stored in application-private storage. Browser requests are not sent to a filter-list server for decisions. Fetching updates does contact each enabled publisher, which receives the ordinary network request/IP information.

## Attribution, source rights and limitations

**OISD** by Stephan van Ruth is licensed under GNU GPL v3; official list and license: https://oisd.nl/faq and https://github.com/sjhgvr/oisd/blob/main/LICENSE. Its project says OISD Small focuses mainly on ads and that domain lists cannot replace browser content filters.

**StevenBlack/hosts** is a merged set of contributions with diverse upstream data sources and license terms. Consult the maintained attribution/source tables in https://github.com/StevenBlack/hosts and in each upstream list's own metadata; a single MIT license does not apply to every merged source.

The source-code license of GoreeCloud Browser Mobile is MIT. GoreeCloud does **not** ship copies of these lists inside the GitHub repository or APK; the app fetches them from the publishers at runtime, leaving the external list data under the source owners' rights. Users should respect publisher licenses when redistributing externally cached data. No publisher endorsement is implied.

Before release, verify actual Android device behavior, offline caches, performance, false positives, publisher availability/terms, and user-visible update/error messaging.
