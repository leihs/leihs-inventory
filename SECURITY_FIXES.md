# Security Vulnerability Fixes

Summary of security alerts addressed in this update.

## Ruby Gems (Gemfile.lock)

### 1. Rack - Directory Traversal via Rack::Static (HIGH)

- **CVE:** CVE-2025-27610
- **Issue:** `Rack::Static` does not properly sanitize user-supplied paths, allowing encoded path traversal sequences to access files that should not be publicly accessible.
- **CVSS:** 7.5 (High)
- **Fix:** rack 3.2.4 → **3.2.5**

### 2. Rack - Stored XSS via javascript: filenames in Rack::Directory (MODERATE)

- **CVE:** CVE-2026-25500
- **Issue:** `Rack::Directory` renders filenames as anchor `href` attributes without sanitization. Files with names starting with `javascript:` can execute arbitrary JavaScript when clicked.
- **CVSS:** 5.4 (Medium)
- **Fix:** rack 3.2.4 → **3.2.5**

### 3. Faraday - SSRF via protocol-relative URL host override (MODERATE)

- **CVE:** CVE-2026-25765
- **Issue:** `build_exclusive_url` uses `URI#merge` to combine base URL with user-supplied paths. Protocol-relative URLs (`//evil.com/path`) override the base URL's host, enabling SSRF attacks.
- **CVSS:** 5.8 (Medium)
- **Fix:** faraday 1.10.4 → **2.14.1**
- **Breaking change:** `faraday_middleware` gem removed (deprecated, incompatible with Faraday 2.x). Replaced with `faraday-multipart` for multipart request support. JSON response parsing and URL encoding are now built into Faraday 2.x core.

### 4. Rack::Session - Session restored after deletion (MODERATE)

- **CVE:** CVE-2025-46336
- **Issue:** When using `Rack::Session::Pool`, concurrent requests can restore a deleted session due to a race condition — the session is loaded at request start and saved back after processing, even if another request deleted it in between.
- **CVSS:** 4.2 (Medium)
- **Fix:** rack-session 2.0.0 → **2.1.1**

### 5. rails-html-sanitizer - XSS with certain configurations (LOW, multiple CVEs)

- **CVEs:** CVE-2024-53988, CVE-2024-53989, CVE-2024-53990, CVE-2024-53991, CVE-2024-53992
- **Issue:** Multiple XSS vectors when HTML5 sanitization is enabled and the allowed tags include specific combinations of math/table/style elements.
- **Fix:** rails-html-sanitizer 1.6.0 → **1.6.2**

## npm (package-lock.json)

### 6. Lodash - Prototype Pollution in _.unset and _.omit (MODERATE)

- **CVE:** CVE-2025-13465
- **Issue:** Specially crafted paths passed to `_.unset` and `_.omit` can delete methods and properties from global prototypes, destabilizing applications.
- **CVSS:** 5.3 (Medium)
- **Fix:** lodash 4.17.21 → **4.17.23** (via `npm audit fix`)
- **Scope:** Development dependency only

### 7. qs - arrayLimit bypass in comma parsing (LOW)

- **CVE:** CVE-2026-2391
- **Issue:** When `comma: true` is enabled, the `arrayLimit` check occurs after comma splitting, allowing attackers to create arbitrarily large arrays from a single parameter, causing memory exhaustion (DoS).
- **CVSS:** 6.3 (Medium)
- **Fix:** qs 6.14.1 → **6.15.0** (via `npm audit fix`)
- **Scope:** Development dependency only

### 8. Elliptic - Risky Cryptographic Implementation (LOW)

- **CVE:** CVE-2025-14505
- **Issue:** ECDSA implementation generates incorrect signatures when the interim 'k' value has leading zeros, potentially enabling secret key exposure under certain conditions.
- **CVSS:** 5.6 (Medium)
- **Status:** NO FIX AVAILABLE — all versions up to 6.6.1 are affected. The package is a transitive dependency of `shadow-cljs` (via `node-libs-browser` → `crypto-browserify` → `browserify-sign`). Upgrading `shadow-cljs` from 2.26.3 to >= 2.28.24 would resolve this, but constitutes a breaking change.
- **Scope:** Development dependency only
- **Mitigation:** Low risk as this only affects the dev build toolchain, not production code.

## Files Changed

| File | Change |
|------|--------|
| `Gemfile` | Replaced `faraday`/`faraday_middleware` with `faraday (>= 2.14.1)`/`faraday-multipart` |
| `Gemfile.lock` | Updated rack, rack-session, faraday, rails-html-sanitizer, loofah, nokogiri |
| `database/Gemfile` | Added minimum version pins for rack, rack-session, rails-html-sanitizer |
| `package-lock.json` | Updated lodash (4.17.23), qs (6.15.0) |
| `spec/config/web.rb` | Changed `require "faraday_middleware"` to `require "faraday/multipart"` |
