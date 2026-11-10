# Search provider adoption policy

Current implementation: **Startpage is the only search engine** in this development preview. The active engine is fixed in `SearchPolicy`; its HTTPS endpoint is `https://www.startpage.com/do/search?query=` with properly encoded queries.

GoreeCloud Search repository state and actual endpoint readiness must be reverified before any change. A repository, prototype or unmerged pull request is **not** evidence of stability.

## Gate for switching to GoreeCloud Search

1. Default-branch implementation and reproducible automated tests exist.
2. Public HTTPS endpoint, transport security, query encoding, query retention and privacy rules are verified.
3. Security review, abuse resistance, stability, representative performance, and accessibility checks pass.
4. Search quality and core user workflows are tested against Startpage on actual Android devices.
5. The owner approves a rollout, migration/rollback plan and documentation updates.
6. Updated app builds and device behavior are verified. Switching is explicit and not time-triggered.

Until every applicable gate passes, Startpage remains the fallback and sole configured search provider. Do not invent a GoreeCloud Search URL.

## Bounded navigation input

The native address/intent route rejects text longer than 4,096 characters **before** trimming or encoding it as a Startpage query or HTTPS URL. This limits resource amplification from pasted text and external `ACTION_VIEW` data; it does not constrain normal valid browsing, replace Android intent verification, or make Startpage searches private by itself. Malformed explicit URLs and unsupported URI schemes remain subject to the existing fail-closed controls.
