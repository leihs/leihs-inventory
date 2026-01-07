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
   → 404 text/plain if route doesn't exist (no SPA)

4. No acceptable format
   → 406 Not Acceptable
```

### Accept Whitelist

Maintain whitelist of supported Accept types:
- `text/html`
- `application/json`
- `image/*`

Behavior:
- **No Accept header or `*/*`:** Return default for resource (text/html for inventory routes)
- **Unsupported Accept:** Return 400 or 406 (request cannot be processed)

Example invalid request:
```
POST /some-endpoint
Content-Type: application/json
Accept: application/xml

{ "age": "twenty" }
```
→ 406 or 400 (no endpoint returns application/xml)

## Error Handling by Accept Header

### Accept: application/json

Error codes for JSON API endpoints (e.g., GET /inventory/:pool-id/models/:model-id):
- **404** - Resource not found
- **400** - Malformed request (invalid headers, JSON syntax error, missing required fields, invalid query string)
- **422** - Validation error (invalid property types in payload)

### Accept: image/*

For image endpoints:
- **404 text/plain** - Image not found
  - Content-Type: text/plain
  - Body: "image not found"
  - **No SPA fallback**
- **406** - Unsupported image type requested

### Accept: text/html or */*

For `/inventory/*` routes:
- **200 SPA** - All routes, regardless of backend route existence
- Client-side routing handles actual route resolution

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

`custom-not-found-handler` in `resource_handler.clj:9-15`:

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

**Changes:** `resource_handler.clj:14`
- HTML requests: status 404 → 200
- Image requests: return text/plain (no SPA)
- Unsupported Accept: return 406

```clojure
(def supported-accepts #{"text/html" "application/json" "image/"})

(defn custom-not-found-handler [request]
  (let [accept (get-in request [:headers "accept"] "*/*")
        uri (:uri request)
        is-html? (or (str/includes? accept "text/html")
                     (str/includes? accept "*/*"))
        is-image? (str/includes? accept "image/")
        is-inventory? (str/includes? uri "/inventory")
        supported? (or (= accept "*/*")
                       (some #(str/includes? accept %) supported-accepts))]
    (cond
      ;; Unsupported Accept header
      (not supported?)
      {:status 406
       :headers {"content-type" "text/plain"}
       :body "Not Acceptable"}

      ;; HTML + inventory → SPA/200
      (and is-html? is-inventory?)
      (rh/index-html-response request 200)  ; ← Changed to 200

      ;; Image → text/plain (no SPA)
      is-image?
      {:status 404
       :headers {"content-type" "text/plain"}
       :body "image not found"}

      ;; Default → JSON
      :else
      {:status 404
       :headers {"content-type" "application/json"}
       :body {:error "Not Found"}})))
```

**Benefits:**
- Minimal changes to existing architecture
- Handles all Accept types correctly
- Accept whitelist validation

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

Changes to `resource_handler.clj`:
- Add Accept whitelist validation (406 for unsupported types)
- Change 404→200 for HTML inventory requests
- Return text/plain for image/* 404s (no SPA)
- Maintain JSON 404s for API endpoints

## Implementation Recommendations

### Simplified Middleware

```clojure
(def supported-accepts #{"text/html" "application/json" "image/"})

(defn wrap-content-negotiate [handler]
  (fn [request]
    (let [accept (get-in request [:headers "accept"] "*/*")
          uri (:uri request)
          is-inventory? (re-matches #"/inventory(/.*)?" uri)
          is-html? (or (str/includes? accept "text/html")
                       (str/includes? accept "*/*"))
          is-image? (str/includes? accept "image/")
          is-json? (str/includes? accept "application/json")
          supported? (or (= accept "*/*")
                         (some #(str/includes? accept %) supported-accepts))]

      (cond
        ;; Unsupported Accept header
        (and (not= accept "*/*") (not supported?))
        {:status 406
         :headers {"content-type" "text/plain"}
         :body "Not Acceptable"}

        ;; Inventory + HTML → Always SPA/200
        (and is-inventory? is-html?)
        (rh/index-html-response request 200)

        ;; Handle request
        :else
        (let [resp (handler request)]
          (if (= 404 (:status resp))
            (cond
              ;; Image 404 → text/plain (no SPA)
              is-image?
              {:status 404
               :headers {"content-type" "text/plain"}
               :body "image not found"}

              ;; JSON 404
              is-json?
              {:status 404
               :headers {"content-type" "application/json"}
               :body {:error "Not found"}}

              ;; Default
              :else resp)
            resp))))))
```

### Benefits

- No route existence checking for HTML requests
- SPA handles all client routing
- API requests (JSON) get proper 404
- Image 404s return text/plain (no SPA)
- Accept whitelist validation (406 for unsupported types)
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

## Issue #2063 Updates

Based on discussion in issue comments, the following have been clarified and documented:

1. **Client-side routing case:** Route doesn't exist but HTML requested → SPA/200 ✓
2. **Image handling:** image/* 404 returns text/plain "image not found" (no SPA) ✓
3. **406 responses:** Accept whitelist approach - reject unsupported Accept headers ✓
4. **Error codes by Accept type:** Different status codes (400/404/422) for JSON API endpoints ✓
5. **Precedence ordering:** HTML first, then JSON, then image/* ✓

## Related Files

- `src/leihs/inventory/server/app_handler.clj` - Middleware stack and router initialization
- `src/leihs/inventory/server/utils/middleware_handler.clj` - Content negotiation logic
- `src/leihs/inventory/server/utils/resource_handler.clj` - Not-found handler (content negotiation for unmatched routes)
- `src/leihs/inventory/server/utils/response_helper.clj` - Response helpers
- `src/leihs/inventory/server/resources/routes.clj` - Route definitions

## References

- [RFC 9110 Content Negotiation](https://www.rfc-editor.org/rfc/rfc9110.html#name-content-negotiation)
