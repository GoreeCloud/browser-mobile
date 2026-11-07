# Browser family identity — Android implementation

**Design approval:** The founder explicitly approved **Prism Tabs v5**, not the older ring-arrow/window alternatives. The central source was promoted through [GoreeCloud/brand-assets PR #10](https://github.com/GoreeCloud/brand-assets/pull/10), merged at commit [1b12ecb](https://github.com/GoreeCloud/brand-assets/commit/1b12ecbb043c89b4f461e68ce2cd83b5fd70eb3f). The **same icon master** is also used by the Linux-first `GoreeCloud/browser` desktop app. Neither product is released as Stable.

## Source and Android resource mapping

| Consumer resource | Verified central source | Role |
| --- | --- | --- |
| `branding/prism-tabs-v5.svg` | `products/browser-mobile/app-icon.svg` | Byte-identical founder-approved color SVG (Git blob `c82371aa6fadd45eb26610bf4907e64122d15b12`) |
| `branding/prism-tabs-v5-symbol.svg` | `products/browser-mobile/symbol.svg` | Byte-identical transparent foreground (Git blob `d5f96e5dfac25588f4c4a95ac3277adc33d3cafa`) |
| `branding/prism-tabs-v5-monochrome.svg` | `products/browser-mobile/monochrome.svg` | Derived white/alpha silhouette for Android themed icons (Git blob `56fd59404c83ac8c26416c3a824f8511cf2191d2`) |
| `ic_browser_symbol.png` | `raster/icon-512.png` | Full approved color icon in the native Compose header |
| `ic_launcher_foreground.png` | `raster/android-foreground-512.png` | 392px transparent SVG resized with 60px padding on all sides to meet the adaptive-icon safe region |
| `ic_launcher_monochrome.png` | `raster/android-monochrome-512.png` | Themed launcher derivative, similarly padded for safe adaptive-icon masks |

The existing Android manifest and adaptive-icon resource continue to resolve `@mipmap/ic_launcher`; the 108dp adaptive background is GoreeCloud navy `@color/icon_bg`. The PNG copies are *checked-in package resources* for offline use; the source remains central in `brand-assets`. No external network, image-generation service, embedded commercial browser mark or font file is required.

**Integrity:** `python3 scripts/check_brand_assets.py` verifies exact central Git SVG blob fingerprints, the pinned `branding/asset-index.json` SHA-256 and file-size values, PNG signatures/512px dimensions and Android launcher resource links. GitHub Android CI runs it before unit tests, APK assembly and Lint.

**On-device preview:** Local debug APK was signed with Android v2 and upgraded in-place on the authorized OnePlus N200 (Android 16), preserving the existing package/app data. The header renders the original color icon and Android's App Info shows the launcher icon under a circular mask. These are **bounded visual smoke checks**, not a universal OEM mask, themed launcher, accessibility, Play Store, release signing or Stable validation. Screenshots containing device UI were kept local to the authorized test computer rather than committed publicly.

The full/color icon's **founder aesthetic approval** does not separately approve monochrome tinting, tiny-size mask behavior or all platform-specific derivatives. Revisit those if device testing reveals regressions.
