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

## Reitit Architecture Considerations

### The Challenge

Reitit applies middleware only to **matched routes**. For unmatched routes, middleware stack doesn't run.

**Flow:**
```
Request → Router → Match?
                    ├─ Yes → Route middleware → Handler
                    └─ No  → :not-found handler (no route middleware)
```

For content negotiation with SPA fallback, need to handle unmatched routes.

### Current Implementation

Uses **`:not-found` handler** approach in `app_handler.clj:92-94`:

```clojure
(ring/ring-handler router
  (ring/create-default-handler
    {:not-found custom-not-found-handler
     :method-not-allowed custom-not-found-handler}))
```

`custom-not-found-handler` in `ressource_handler.clj:9-15`:

```clojure
(defn custom-not-found-handler [request]
  (let [accept (str/lower-case (or (get-in request [:headers "accept"]) ""))
        uri (:uri request)
        inventory-route? (str/includes? uri "/inventory")]
    (if (and (str/includes? accept "text/html") inventory-route?)
      (rh/index-html-response request 404)  ; ← Issue: 404 not 200
      (create-response-by-accept accept 404 {:error "Not Found"}))))
```

**Problem:** Returns SPA with 404 status. Should be 200 for client routing.

### Three Implementation Options

#### Option 1: Fix :not-found Handler (Recommended)

**Change:** `ressource_handler.clj:14` - status 404 → 200

```clojure
(defn custom-not-found-handler [request]
  (let [accept (get-in request [:headers "accept"] "")
        uri (:uri request)
        is-html? (or (str/includes? accept "text/html")
                     (str/includes? accept "*/*"))
        is-inventory? (str/includes? uri "/inventory")]
    (if (and is-html? is-inventory?)
      (rh/index-html-response request 200)  ; ← Changed to 200
      {:status 404
       :headers {"content-type" "application/json"}
       :body {:error "Not Found"}})))
```

**Benefits:**
- Single line change
- Matches current architecture
- Minimal refactoring

#### Option 2: Outer Middleware Wrapper

Wrap entire app before router processes request:

```clojure
(defn wrap-content-negotiate [handler]
  (fn [request]
    (let [accept (get-in request [:headers "accept"])
          uri (:uri request)]
      (if (and (str/includes? uri "/inventory")
               (or (str/includes? accept "text/html")
                   (str/includes? accept "*/*")))
        (let [resp (handler request)]
          (if (#{404 405} (:status resp))
            (rh/index-html-response request 200)
            resp))
        (handler request)))))

;; In init:
(-> app
    wrap-content-negotiate  ; Add before cache-buster
    (cache-buster2/wrap-resource ...))
```

**Benefits:**
- Runs for all requests
- Can intercept before route matching
- More control over flow

**Drawbacks:**
- More complex
- Duplicates logic from :not-found handler

#### Option 3: Catch-all Route

Add fallback route at end:

```clojure
;; In routes.clj
["/inventory/*" {:fallback? true
                 :get {:handler (fn [req] (rh/index-html-response req 200))}}]
```

**Benefits:**
- Uses Reitit route matching

**Drawbacks:**
- Only handles GET
- Requires :fallback? flag
- Less flexible than handler approach

### Recommendation

**Use Option 1** - Fix existing `:not-found` handler.

One line change: `ressource_handler.clj:14` change 404→200 for HTML inventory requests.

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

- `src/leihs/inventory/server/app_handler.clj` - Middleware stack and router initialization
- `src/leihs/inventory/server/utils/middleware_handler.clj` - Content negotiation logic
- `src/leihs/inventory/server/utils/ressource_handler.clj` - Not-found handler (content negotiation for unmatched routes)
- `src/leihs/inventory/server/utils/response_helper.clj` - Response helpers
- `src/leihs/inventory/server/resources/routes.clj` - Route definitions

## References

- [RFC 9110 Content Negotiation](https://www.rfc-editor.org/rfc/rfc9110.html#name-content-negotiation)
