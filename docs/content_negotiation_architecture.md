# Content Negotiation Architecture

**Date:** 2024-12-24
**Related Issue:** [leihs/leihs#2063](https://github.com/leihs/leihs/issues/2063)

## Overview

Backend request architecture for handling multiple response types (SPA/HTML, JSON APIs, binary assets) from same service via content negotiation at middleware level.

## Current Implementation

### Middleware Stack
Location: `src/leihs/inventory/server/app_handler.clj:35-68`

Content negotiation handled by two middleware:

1. **`wrap-strict-format-negotiate`** (line 40)
   - Pre-handler format negotiation
   - Route existence checking
   - Format matching against endpoint `:produces`
   - Location: `middleware_handler.clj:67-100`

2. **`wrap-html-40x`** (line 36)
   - Post-handler error conversion
   - Converts 40x responses to SPA HTML for matching URIs
   - Location: `middleware_handler.clj:102-116`

### Current Issues

- Complex conditional logic hard to follow
- Mixed concerns: route checking + format negotiation
- Two separate middleware for related functionality
- Status code calculation unclear (`resp-status` lines 89-94)

## Simplified Architecture Principle

### Core Rule

**Any route under `/inventory/*` with `Accept: text/html` or `*/*` → SPA with 200 status**

This enables:
- Client-side routing in SPA
- No backend route existence check needed for HTML requests
- Clear separation: HTML always gets SPA, APIs get structured responses

### Content Negotiation Precedence

```
1. Accept: text/html or */* + /inventory/*
   → SPA (200) - Always, regardless of route existence

2. Accept: application/json
   → JSON endpoint if route exists
   → 404 JSON if route doesn't exist

3. Accept: image/*
   → Binary if image route exists
   → 404 or SPA fallback (if */* also in Accept)

4. No acceptable format
   → 406 Not Acceptable
```

## Implementation Recommendations

### Simplified Middleware

```clojure
(defn wrap-content-negotiate [handler]
  (fn [request]
    (let [accept (get-in request [:headers "accept"])
          uri (:uri request)
          is-inventory? (re-matches #"/inventory(/.*)?" uri)]

      (cond
        ;; Inventory + HTML → Always SPA/200
        (and is-inventory?
             (or (str/includes? accept "text/html")
                 (str/includes? accept "*/*")))
        (rh/index-html-response request 200)

        ;; JSON/image → Check if endpoint handles it
        :else
        (let [resp (handler request)]
          (if (= 404 (:status resp))
            (-> (response {:error "Not found"})
                (status 404)
                (content-type "application/json"))
            resp))))))
```

### Benefits

- No route existence checking for HTML requests
- SPA handles all client routing
- API requests (JSON) get proper 404
- Clear precedence: HTML first, then specific formats
- Eliminates need for separate `wrap-html-40x` middleware
- Simpler, more maintainable code

## Refactoring Strategy

### Option A: Consolidate
- Merge `wrap-strict-format-negotiate` + `wrap-html-40x` into single middleware
- Implement simplified HTML-first logic
- Add explicit 406 responses
- Add image/* handling

### Option B: Keep Separate
- Refactor `wrap-strict-format-negotiate` with simplified logic
- Keep `wrap-html-40x` for post-processing
- Add missing format support
- Clarify precedence rules

**Recommendation:** Option A - consolidation eliminates complexity.

## Issue #2063 Gaps

The proposal is missing explicit documentation of:

1. **Client-side routing case:** Route doesn't exist but HTML requested → SPA/200
2. **Image handling:** No explicit image/* content negotiation flow
3. **406 responses:** When to return "Not Acceptable"
4. **Precedence ordering:** Which format takes priority when multiple in Accept header

## Related Files

- `src/leihs/inventory/server/app_handler.clj` - Middleware stack
- `src/leihs/inventory/server/utils/middleware_handler.clj` - Content negotiation logic
- `src/leihs/inventory/server/utils/response_helper.clj` - Response helpers
- `src/leihs/inventory/server/resources/routes.clj` - Route definitions

## References

- [RFC 9110 Content Negotiation](https://www.rfc-editor.org/rfc/rfc9110.html#name-content-negotiation)
